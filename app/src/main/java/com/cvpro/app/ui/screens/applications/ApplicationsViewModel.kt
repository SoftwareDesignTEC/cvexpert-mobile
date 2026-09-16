package com.cvpro.app.ui.screens.applications

import androidx.lifecycle.ViewModel
import com.cvpro.app.domain.model.ApplicationStatus
import com.cvpro.app.domain.model.JobApplication
import com.cvpro.app.domain.model.JobModality
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * Estado del tablero de postulaciones.
 *
 * @property selectedStatus columna que se está viendo. En la web las cinco columnas se ven a
 * la vez; en el teléfono no caben, así que se muestran como pestañas y se ve una a la vez.
 */
data class ApplicationsUiState(
    val applications: List<JobApplication> = emptyList(),
    val selectedStatus: ApplicationStatus = ApplicationStatus.APPLIED,
    val isLoading: Boolean = false,
) {
    /** Postulaciones de la columna abierta. */
    val visibleApplications: List<JobApplication> =
        applications.filter { it.status == selectedStatus }

    /** Cuántas hay en cada columna, para el número que va en cada pestaña. */
    fun countFor(status: ApplicationStatus): Int = applications.count { it.status == status }
}

@HiltViewModel
class ApplicationsViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(ApplicationsUiState(applications = SAMPLE_APPLICATIONS))
    val uiState: StateFlow<ApplicationsUiState> = _uiState.asStateFlow()

    fun onStatusSelected(status: ApplicationStatus) {
        _uiState.update { it.copy(selectedStatus = status) }
    }

    private companion object {
        /**
         * Postulaciones de prueba para poder armar y ver el tablero.
         *
         * TODO: reemplazar por el repositorio contra el APIM, igual que las ofertas.
         */
        val SAMPLE_APPLICATIONS = listOf(
            JobApplication("1", "Cajero de tienda", "Automercado", "Heredia", JobModality.ON_SITE, ApplicationStatus.APPLIED, "12/09/2026"),
            JobApplication("2", "Asistente administrativo", "Grupo Monge", "San José", JobModality.ON_SITE, ApplicationStatus.APPLIED, "11/09/2026"),
            JobApplication("3", "Agente de servicio al cliente", "Teleperformance", "Heredia", JobModality.HYBRID, ApplicationStatus.IN_PROGRESS, "08/09/2026"),
            JobApplication("4", "Practicante de mercadeo digital", "Pozuelo", "San José", JobModality.HYBRID, ApplicationStatus.INTERVIEW, "05/09/2026"),
            JobApplication("5", "Auxiliar contable", "Despacho Carvajal", "Heredia", JobModality.HYBRID, ApplicationStatus.REJECTED, "01/09/2026"),
            JobApplication("6", "Community manager junior", "Agencia Nova", "Todo el país", JobModality.REMOTE, ApplicationStatus.ACCEPTED, "28/08/2026"),
        )
    }
}
