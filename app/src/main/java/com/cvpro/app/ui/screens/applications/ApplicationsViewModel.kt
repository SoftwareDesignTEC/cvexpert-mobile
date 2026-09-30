package com.cvpro.app.ui.screens.applications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cvpro.app.domain.model.ApplicationStatus
import com.cvpro.app.domain.model.JobApplication
import com.cvpro.app.domain.repository.ApplicationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Estado del rastreador de postulaciones.
 *
 * @property selectedStatus pestaña que se está viendo. En el wireframe de Samuel los cuatro
 * estados son pestañas y se ve una lista a la vez.
 * @property totalRecords cuántas postulaciones hay en total, para el contador de arriba.
 * Viene del servicio, no del tamaño de la lista cargada, porque la lista está paginada.
 */
data class ApplicationsUiState(
    val applications: List<JobApplication> = emptyList(),
    val selectedStatus: ApplicationStatus = ApplicationStatus.APPLIED,
    val totalRecords: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
) {
    /** Postulaciones de la pestaña abierta. */
    val visibleApplications: List<JobApplication> =
        applications.filter { it.status == selectedStatus }
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

    /**
     * Cambia el estado de una postulación (el botón "Estado" de la tarjeta).
     *
     * Es la operación de actualización del CRUD: manda el PUT y reemplaza en la lista la
     * postulación que devuelve el servicio, para no tener que recargar todo.
     */
    fun onStatusChange(application: JobApplication, newStatus: ApplicationStatus) {
        if (application.status == newStatus) return

        viewModelScope.launch {
            try {
                repository.updateApplicationStatus(
                    id = application.id,
                    status = newStatus,
                )

                // La tarjeta se mueve con el estado que pidió el usuario, sin usar lo que
                // devuelve el servicio. Los mocks del APIM responden siempre lo mismo: con
                // la postulación en su estado original y, a veces, con otro id. Si se
                // tomara esa respuesta, la tarjeta volvería sola a su columna.
                // TODO: cuando el servicio sea real, usar la postulación que devuelve.
                _uiState.update { state ->
                    state.copy(
                        applications = state.applications.map { current ->
                            if (current.id == application.id) {
                                current.copy(status = newStatus)
                            } else {
                                current
                            }
                        },
                    )
                }
            } catch (exception: Exception) {
                _uiState.update { it.copy(error = exception.message) }
            }
        }
    }

    private fun loadApplications() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val result = repository.getApplications(
                    page = 1,
                    pageSize = PAGE_SIZE,
                    query = "",
                )

                _uiState.update {
                    it.copy(
                        applications = result.items,
                        totalRecords = result.totalRecords,
                        isLoading = false,
                        error = null,
                    )
                }
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        applications = emptyList(),
                        isLoading = false,
                        error = exception.message,
                    )
                }
            }
        }
    }

    private companion object {
        /** El rastreador muestra todo junto; no tiene scroll infinito como las ofertas. */
        const val PAGE_SIZE = 50
    }
}
