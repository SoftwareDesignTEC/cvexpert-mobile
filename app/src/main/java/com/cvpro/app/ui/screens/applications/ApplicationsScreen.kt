package com.cvpro.app.ui.screens.applications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cvpro.app.R
import com.cvpro.app.domain.model.ApplicationStatus
import com.cvpro.app.domain.model.JobApplication
import com.cvpro.app.domain.model.JobModality
import com.cvpro.app.ui.components.EmptyState
import com.cvpro.app.ui.theme.CvProTheme

/** Rastreador de postulaciones con estado. */
@Composable
fun ApplicationsScreen(
    modifier: Modifier = Modifier,
    viewModel: ApplicationsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ApplicationsBoard(
        uiState = uiState,
        onStatusSelected = viewModel::onStatusSelected,
        modifier = modifier,
    )
}

/**
 * Tablero de postulaciones.
 *
 * Las cuatro columnas del diseño se convierten en pestañas, con el número de
 * postulaciones de cada una. Cada pestaña muestra su lista, o el estado vacío si no tiene.
 */
@Composable
fun ApplicationsBoard(
    uiState: ApplicationsUiState,
    onStatusSelected: (ApplicationStatus) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        PrimaryScrollableTabRow(
            selectedTabIndex = ApplicationStatus.entries.indexOf(uiState.selectedStatus),
            edgePadding = 8.dp,
        ) {
            ApplicationStatus.entries.forEach { status ->
                Tab(
                    selected = status == uiState.selectedStatus,
                    onClick = { onStatusSelected(status) },
                    text = {
                        Text(
                            text = stringResource(
                                R.string.applications_tab_with_count,
                                stringResource(status.labelRes()),
                                uiState.countFor(status),
                            ),
                        )
                    },
                )
            }
        }

        val applications = uiState.visibleApplications
        if (applications.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.applications_empty_title),
                message = stringResource(
                    R.string.applications_empty_message,
                    stringResource(uiState.selectedStatus.labelRes()),
                ),
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(items = applications, key = { it.id }) { application ->
                    ApplicationCard(application = application)
                }
            }
        }
    }
}

/** Tarjeta de una postulación, con lo mismo que muestra la tarjeta del tablero web. */
@Composable
private fun ApplicationCard(application: JobApplication, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = application.jobTitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = application.company,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(
                    R.string.jobs_location_and_modality,
                    application.location,
                    stringResource(application.modality.labelRes()),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = stringResource(R.string.applications_applied_at, application.appliedAt),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

private fun ApplicationStatus.labelRes(): Int = when (this) {
    ApplicationStatus.APPLIED -> R.string.application_status_applied
    ApplicationStatus.IN_PROGRESS -> R.string.application_status_in_progress
    ApplicationStatus.INTERVIEW -> R.string.application_status_interview
    ApplicationStatus.REJECTED -> R.string.application_status_rejected
}

private fun JobModality.labelRes(): Int = when (this) {
    JobModality.REMOTE -> R.string.modality_remote
    JobModality.ON_SITE -> R.string.modality_on_site
    JobModality.HYBRID -> R.string.modality_hybrid
}

@Preview(name = "Postulaciones")
@Composable
private fun ApplicationsBoardPreview() {
    CvProTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            ApplicationsBoard(
                uiState = ApplicationsUiState(
                    applications = listOf(
                        JobApplication("1", "Cajero de tienda", "Automercado", "Heredia", JobModality.ON_SITE, ApplicationStatus.APPLIED, "12/09/2026"),
                    ),
                ),
                onStatusSelected = {},
            )
        }
    }
}
