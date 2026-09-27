package com.cvpro.app.ui.screens.applications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cvpro.app.domain.model.ApplicationStatus
import com.cvpro.app.domain.model.JobApplication
import com.cvpro.app.domain.repository.ApplicationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import com.cvpro.app.domain.model.JobModality
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Estado del tablero de postulaciones.
 *
 * @property selectedStatus columna que se está viendo. En la web las columnas se ven todas a
 * la vez; en el teléfono no caben, así que se muestran como pestañas y se ve una a la vez.
 */
data class ApplicationsUiState(
    val applications: List<JobApplication> = emptyList(),
    val selectedStatus: ApplicationStatus = ApplicationStatus.APPLIED,
    val isLoading: Boolean = false,
    val error: String? = null,
) {
    /** Postulaciones de la columna abierta. */
    val visibleApplications: List<JobApplication> =
        applications.filter { it.status == selectedStatus }

    /** Cuántas hay en cada columna, para el número que va en cada pestaña. */
    fun countFor(status: ApplicationStatus): Int = applications.count { it.status == status }
}

@HiltViewModel
class ApplicationsViewModel @Inject constructor(
    private val repository: ApplicationRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ApplicationsUiState())
    val uiState: StateFlow<ApplicationsUiState> = _uiState.asStateFlow()

    init { loadApplications() }

    fun onStatusSelected(status: ApplicationStatus) {
        _uiState.update { it.copy(selectedStatus = status) }
    }

    fun retry() {
        loadApplications()
    }

    private fun loadApplications() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                )
            }

            try {
                val result = repository.getApplications(
                    page = 1,
                    pageSize = 50,
                    query = "",
                )

                _uiState.update {
                    it.copy(
                        applications = result.items,
                        isLoading = false,
                        error = null,
                    )
                }
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        applications = emptyList(),
                        isLoading = false,
                        error = exception.message ?: "Error al cargar las postulaciones",
                    )
                }
            }
        }
    }
}
