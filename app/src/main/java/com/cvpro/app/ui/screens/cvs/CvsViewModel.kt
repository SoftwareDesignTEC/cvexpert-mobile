package com.cvpro.app.ui.screens.cvs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cvpro.app.domain.model.Cv
import com.cvpro.app.domain.repository.CvRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CvsUiState(
    val cvs: List<Cv> = emptyList(),
    val query: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val deletingId: String? = null,
)

@HiltViewModel
class CvsViewModel @Inject constructor(
    private val repository: CvRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CvsUiState())
    val uiState: StateFlow<CvsUiState> = _uiState.asStateFlow()

    /** Espera de la búsqueda, para no pedirle al servicio en cada tecla. */
    private var searchJob: Job? = null

    init {
        loadCvs()
    }

    fun retry() {
        loadCvs()
    }

    /**
     * Texto del buscador.
     *
     * No pide al servicio en cada tecla: cancela la búsqueda anterior y espera un momento a
     * que el usuario deje de escribir.
     */
    fun onQueryChange(value: String) {
        _uiState.update { it.copy(query = value) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(SEARCH_DELAY_MS)
            loadCvs()
        }
    }

    fun deleteCv(id: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    deletingId = id,
                    error = null,
                )
            }

            try {
                repository.deleteCv(id)

                _uiState.update { state ->
                    state.copy(
                        cvs = state.cvs.filterNot { it.id == id },
                        deletingId = null,
                    )
                }
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        deletingId = null,
                        error = exception.message ?: "Error al eliminar el CV",
                    )
                }
            }
        }
    }

    private fun loadCvs() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                )
            }

            try {
                val result = repository.getCvs(
                    page = 1,
                    pageSize = PAGE_SIZE,
                    query = _uiState.value.query.trim(),
                )

                _uiState.update {
                    it.copy(
                        cvs = result.items,
                        isLoading = false,
                        error = null,
                    )
                }
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        cvs = emptyList(),
                        isLoading = false,
                        error = exception.message ?: "Error al cargar los CV",
                    )
                }
            }
        }
    }

    private companion object {
        const val PAGE_SIZE = 50
        const val SEARCH_DELAY_MS = 350L
    }
}