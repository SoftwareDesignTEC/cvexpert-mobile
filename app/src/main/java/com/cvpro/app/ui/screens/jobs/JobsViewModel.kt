package com.cvpro.app.ui.screens.jobs

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cvpro.app.R
import com.cvpro.app.domain.model.JobOffer
import com.cvpro.app.domain.repository.JobOfferRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
) {
    /** Sin error y sin resultados: hay que mostrar el estado vacío, no una lista en blanco. */
    val isEmpty: Boolean = !isLoading && errorMessage == null && offers.isEmpty()
}

@HiltViewModel
class JobsViewModel @Inject constructor(
    private val jobOfferRepository: JobOfferRepository,
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
    }
}
