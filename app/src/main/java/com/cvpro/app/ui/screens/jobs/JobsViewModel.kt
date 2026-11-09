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
) {
    /**
     * Ofertas que se ven con el filtro elegido.
     *
     * El filtro se aplica sobre lo ya cargado, no en el servicio: los mocks del APIM no
     * reciben modalidad como parámetro.
     */
    val visibleOffers: List<JobOffer> = when (selectedFilter) {
        JobsFilter.RECOMMENDED -> offers.sortedByDescending { it.matchScore ?: 0 }
        JobsFilter.REMOTE -> offers.filter { it.modality == JobModality.REMOTE }
        JobsFilter.HYBRID -> offers.filter { it.modality == JobModality.HYBRID }
        JobsFilter.RECENT -> offers
    }

    /** Sin error y sin resultados: hay que mostrar el estado vacío, no una lista en blanco. */
    val isEmpty: Boolean = !isLoading && errorMessage == null && visibleOffers.isEmpty()
}

/**
 * Filtros rápidos del diseño.
 *
 * RECENT está dibujado pero no filtra: el modelo de oferta no trae la fecha de publicación.
 * TODO: pedirle a Pérez que postedAt llegue al dominio para poder filtrar por fecha.
 */
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
     * Postula al usuario a la oferta (POST /applications).
     *
     * El servicio de Ofertas toma el usuario del token y la postulación nace en "Aplicado".
     * Un 409 significa que ya estaba postulado: no es un error para el usuario, así que se
     * le avisa y la oferta queda marcada igual.
     */
    fun onApply(offer: JobOffer) {
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
                    status = ApplicationStatus.APPLIED,
                    notes = null,
                )
                // Con una oferta real, la postulación de verdad se termina en la página de la
                // empresa: se registra en el tablero y se abre esa página.
                val message = if (offer.applyUrl != null) {
                    UiMessage(R.string.jobs_apply_success_external, listOf(offer.title))
                } else {
                    UiMessage(R.string.jobs_apply_success, listOf(offer.title))
                }
                markApplied(offer, message)
                _uiState.update { it.copy(urlToOpen = offer.applyUrl) }
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
            runCatching { jobOfferRepository.getOffers(page = currentPage + 1, query = state.query) }
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

    private fun loadFirstPage() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching { jobOfferRepository.getOffers(page = 1, query = _uiState.value.query) }
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
        /** Igual que el tablero de Postulaciones: alcanza para todas las de un usuario. */
        const val APPLICATIONS_PAGE_SIZE = 50
        /** El servicio de Ofertas responde 409 si el usuario ya se postuló a esa oferta. */
        const val HTTP_CONFLICT = 409
    }
}
