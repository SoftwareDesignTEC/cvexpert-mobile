package com.cvpro.app.ui.screens.jobs

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cvpro.app.R
import com.cvpro.app.domain.model.ApplicationStatus
import com.cvpro.app.domain.model.JobModality
import com.cvpro.app.domain.model.JobOffer
import com.cvpro.app.domain.repository.ApplicationRepository
import com.cvpro.app.domain.repository.AuthRepository
import com.cvpro.app.domain.repository.JobOfferRepository
import com.cvpro.app.ui.components.UiMessage
import com.cvpro.app.ui.components.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

/**
 * Estado de Ofertas Laborales.
 *
 * `isLoading` es la carga de la primera página (o de una búsqueda nueva) y ocupa la
 * pantalla; `isLoadingMore` es la de las páginas siguientes y solo se ve al final de la
 * lista. Se separan porque no se muestran igual.
 */
data class JobsUiState(
    val query: String = "",
    val offers: List<JobOffer> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    @param:StringRes val errorMessage: Int? = null,
    val totalRecords: Int = 0,
    val hasMore: Boolean = false,
    val selectedFilter: JobsFilter = JobsFilter.RECOMMENDED,
    /** Oferta cuya postulación se está enviando, para mostrar el loader en su botón. */
    val applyingOfferId: String? = null,
    /** Ofertas a las que el usuario ya se postuló: su botón queda apagado. */
    val appliedOfferIds: Set<String> = emptySet(),
    /** Resultado de la última postulación: se muestra en el snackbar y se limpia. */
    val message: UiMessage? = null,
    /** Página real de la oferta que hay que abrir en el navegador (después de postularse). */
    val urlToOpen: String? = null,
    /** Se están buscando ofertas nuevas en JSearch ("Buscar ofertas nuevas"). */
    val isRefreshing: Boolean = false,
) {
    /**
     * Ofertas que se ven con el filtro elegido.
     *
     * El filtro se aplica sobre lo ya cargado, no en el servicio: la ruta /jobs no recibe
     * modalidad ni fecha. Por eso la pantalla pide páginas grandes (ver JOBS_PAGE_SIZE): así
     * el filtro ve todas las ofertas y no solo las primeras diez.
     */
    val visibleOffers: List<JobOffer> = when (selectedFilter) {
        JobsFilter.RECOMMENDED -> offers.sortedByDescending { it.matchScore ?: 0 }
        JobsFilter.REMOTE -> offers.filter { it.modality == JobModality.REMOTE }
        JobsFilter.HYBRID -> offers.filter { it.modality == JobModality.HYBRID }
        JobsFilter.RECENT -> offers
            .filter { (it.postedAtMillis ?: 0) >= System.currentTimeMillis() - RECENT_WINDOW_MS }
            .sortedByDescending { it.postedAtMillis }
    }

    /** Sin error y sin resultados: hay que mostrar el estado vacío, no una lista en blanco. */
    val isEmpty: Boolean = !isLoading && errorMessage == null && visibleOffers.isEmpty()
}

/** "Últimos 7 días", en milisegundos. */
private const val RECENT_WINDOW_MS = 7L * 24 * 60 * 60 * 1000

/** Filtros rápidos del diseño. */
enum class JobsFilter(@param:StringRes val labelRes: Int) {
    RECOMMENDED(R.string.jobs_filter_recommended),
    REMOTE(R.string.jobs_filter_remote),
    HYBRID(R.string.jobs_filter_hybrid),
    RECENT(R.string.jobs_filter_recent),
}

@HiltViewModel
class JobsViewModel @Inject constructor(
    private val jobOfferRepository: JobOfferRepository,
    private val applicationRepository: ApplicationRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(JobsUiState())
    val uiState: StateFlow<JobsUiState> = _uiState.asStateFlow()

    private var currentPage = 0
    private var searchJob: Job? = null

    init {
        loadFirstPage()
    }

    /**
     * Cada letra que se escribe reinicia una espera corta antes de buscar. Así no se dispara
     * una búsqueda por tecla, que con el servicio real serían muchas llamadas al pedo.
     */
    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            loadFirstPage()
        }
    }

    fun onRetryClick() = loadFirstPage()

    /**
     * Se llama cada vez que la pantalla aparece: pide las postulaciones del usuario para
     * marcar las ofertas a las que ya se postuló. Se repite al volver porque desde el
     * tablero de Postulaciones pudo haber cambiado algo.
     */
    fun onScreenShown() {
        // Si ya había ofertas, se vuelven a pedir sin indicador de carga: el servicio importa
        // ofertas nuevas cada dos días y el menú conserva esta pantalla con lo de antes.
        if (_uiState.value.offers.isNotEmpty()) loadFirstPage(showLoader = false)
        viewModelScope.launch {
            try {
                val applications = applicationRepository.getApplications(
                    page = 1,
                    pageSize = APPLICATIONS_PAGE_SIZE,
                    query = "",
                )
                val ids = applications.items.map { it.jobOfferId }.filter { it.isNotBlank() }
                _uiState.update { it.copy(appliedOfferIds = ids.toSet()) }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                // Si falla no se muestra error: las ofertas se ven igual y, si el usuario se
                // vuelve a postular, el servicio responde 409 y se marca en ese momento.
            }
        }
    }

    /**
     * "Buscar ofertas nuevas": el servicio corre la búsqueda en JSearch en ese momento y
     * después se recarga la lista. Sirve para mostrar en vivo que las ofertas son reales.
     */
    fun onRefreshOffers() {
        if (_uiState.value.isRefreshing) return
        _uiState.update { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            val message = try {
                val result = jobOfferRepository.refreshOffers()
                loadFirstPage(showLoader = false)
                if (result.created > 0) {
                    UiMessage(R.string.jobs_refresh_new, listOf(result.created.toString(), result.updated.toString()))
                } else {
                    UiMessage(R.string.jobs_refresh_none, listOf(result.updated.toString()))
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: HttpException) {
                UiMessage(
                    if (exception.code() == HTTP_TOO_MANY_REQUESTS) R.string.jobs_refresh_too_soon else R.string.jobs_refresh_unavailable,
                )
            } catch (exception: IOException) {
                UiMessage(R.string.jobs_error_network)
            }
            _uiState.update { it.copy(isRefreshing = false, message = message) }
        }
    }

    /**
     * Agrega la oferta a Mis Postulaciones (POST /applications) en la columna [status], que
     * elige el usuario. Un 409 significa que ya estaba en el tablero: no es un error, así que
     * se le avisa y la oferta queda marcada igual.
     */
    fun onApply(offer: JobOffer, status: ApplicationStatus = ApplicationStatus.APPLIED) {
        val state = _uiState.value
        if (state.applyingOfferId != null || offer.id in state.appliedOfferIds) return

        viewModelScope.launch {
            _uiState.update { it.copy(applyingOfferId = offer.id) }
            try {
                applicationRepository.createApplication(
                    userId = authRepository.currentUser.value?.id.orEmpty(),
                    jobOfferId = offer.id,
                    jobTitle = offer.title,
                    company = offer.company,
                    companyLogoUrl = offer.companyLogoUrl,
                    location = offer.location,
                    modality = offer.modality,
                    status = status,
                    notes = null,
                )
                // Solo "Aplicado" abre la página de la empresa: ahí se termina de postular. En las
                // demás columnas el usuario solo está ordenando su tablero.
                val opensCompanyPage = status == ApplicationStatus.APPLIED && offer.applyUrl != null
                val message = when {
                    status == ApplicationStatus.INTERESTED -> UiMessage(R.string.jobs_saved_interested, listOf(offer.title))
                    opensCompanyPage -> UiMessage(R.string.jobs_apply_success_external, listOf(offer.title))
                    status == ApplicationStatus.APPLIED -> UiMessage(R.string.jobs_apply_success, listOf(offer.title))
                    else -> UiMessage(R.string.jobs_saved_column, listOf(offer.title))
                }
                markApplied(offer, message)
                if (opensCompanyPage) _uiState.update { it.copy(urlToOpen = offer.applyUrl) }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: HttpException) {
                if (exception.code() == HTTP_CONFLICT) {
                    markApplied(offer, UiMessage(R.string.jobs_apply_already))
                } else {
                    failApply(exception)
                }
            } catch (exception: Exception) {
                failApply(exception)
            }
        }
    }

    fun onMessageShown() {
        _uiState.update { it.copy(message = null) }
    }

    /** La pantalla ya abrió la página; se limpia para no abrirla otra vez al repintarse. */
    fun onUrlOpened() {
        _uiState.update { it.copy(urlToOpen = null) }
    }

    private fun markApplied(offer: JobOffer, message: UiMessage) {
        _uiState.update {
            it.copy(
                applyingOfferId = null,
                appliedOfferIds = it.appliedOfferIds + offer.id,
                message = message,
            )
        }
    }

    private fun failApply(exception: Exception) {
        _uiState.update {
            it.copy(applyingOfferId = null, message = UiMessage(exception.toUserMessage()))
        }
    }

    /** Cambia el filtro rápido. No vuelve a pedir: se aplica sobre lo que ya está cargado. */
    fun onFilterChange(filter: JobsFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    /** Pide la siguiente página. No hace nada si ya está cargando o si no quedan más. */
    fun onLoadMore() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.hasMore) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            runCatching { jobOfferRepository.getOffers(page = currentPage + 1, pageSize = JOBS_PAGE_SIZE, query = state.query) }
                .onSuccess { result ->
                    currentPage = result.page
                    _uiState.update {
                        it.copy(
                            offers = it.offers + result.items,
                            isLoadingMore = false,
                            totalRecords = result.totalRecords,
                            hasMore = result.hasMore,
                        )
                    }
                }
                .onFailure { error ->
                    // Falló solo la página nueva: se deja lo que ya se veía y se avisa.
                    _uiState.update { it.copy(isLoadingMore = false, errorMessage = error.toMessageRes()) }
                }
        }
    }

    private fun loadFirstPage(showLoader: Boolean = true) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = showLoader, errorMessage = null) }
            runCatching { jobOfferRepository.getOffers(page = 1, pageSize = JOBS_PAGE_SIZE, query = _uiState.value.query) }
                .onSuccess { result ->
                    currentPage = result.page
                    _uiState.update {
                        it.copy(
                            offers = result.items,
                            isLoading = false,
                            totalRecords = result.totalRecords,
                            hasMore = result.hasMore,
                        )
                    }
                }
                .onFailure { error ->
                    currentPage = 0
                    _uiState.update {
                        it.copy(
                            offers = emptyList(),
                            isLoading = false,
                            hasMore = false,
                            errorMessage = error.toMessageRes(),
                        )
                    }
                }
        }
    }

    /** Traduce la falla a un mensaje entendible; nunca se muestra la excepción tal cual. */
    @StringRes
    private fun Throwable.toMessageRes(): Int =
        if (this is IOException) R.string.jobs_error_network else R.string.jobs_error_generic

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 350L
        /**
         * Ofertas por página. Alcanza para todas las que importa el servicio (unas 40), así los
         * filtros rápidos trabajan sobre todas; si hay más, sigue el botón "Cargar más".
         */
        const val JOBS_PAGE_SIZE = 50
        /** Igual que el tablero de Postulaciones: alcanza para todas las de un usuario. */
        const val APPLICATIONS_PAGE_SIZE = 50
        /** El servicio de Ofertas responde 409 si el usuario ya se postuló a esa oferta. */
        const val HTTP_CONFLICT = 409
        /** "Buscar ofertas nuevas" se usó hace menos de 10 minutos. */
        const val HTTP_TOO_MANY_REQUESTS = 429
    }
}
