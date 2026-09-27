package com.cvpro.app.ui.screens.templates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cvpro.app.domain.model.CvTemplate
import com.cvpro.app.domain.repository.TemplateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TemplatesUiState(
    val templates: List<CvTemplate> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class TemplatesViewModel @Inject constructor(
    private val repository: TemplateRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TemplatesUiState())
    val uiState: StateFlow<TemplatesUiState> = _uiState.asStateFlow()

    init {
        loadTemplates()
    }

    fun retry() {
        loadTemplates()
    }

    private fun loadTemplates() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                )
            }

            try {
                val result = repository.getTemplates(
                    page = 1,
                    pageSize = 50,
                    query = "",
                )

                _uiState.update {
                    it.copy(
                        templates = result.items,
                        isLoading = false,
                        error = null,
                    )
                }
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        templates = emptyList(),
                        isLoading = false,
                        error = exception.message ?: "Error al cargar las plantillas",
                    )
                }
            }
        }
    }
}