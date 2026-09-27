package com.cvpro.app.ui.screens.cvs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cvpro.app.domain.model.Cv
import com.cvpro.app.domain.repository.CvRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CvsUiState(
    val cvs: List<Cv> = emptyList(),
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

    init {
        loadCvs()
    }

    fun retry() {
        loadCvs()
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
                    pageSize = 50,
                    query = "",
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
}