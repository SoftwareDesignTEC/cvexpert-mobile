package com.cvpro.app.ui.screens.applications

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cvpro.app.R
import com.cvpro.app.data.mapper.daysSince
import com.cvpro.app.domain.model.ApplicationStatus
import com.cvpro.app.domain.model.JobApplication
import com.cvpro.app.domain.model.JobModality
import com.cvpro.app.ui.theme.CvProTheme
import com.cvpro.app.ui.theme.extraColors

/** Rastreador de postulaciones con estado. */
@Composable
fun ApplicationsScreen(
    modifier: Modifier = Modifier,
    viewModel: ApplicationsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ApplicationsTracker(
        uiState = uiState,
        onStatusChange = viewModel::onStatusChange,
        onRetry = viewModel::retry,
        modifier = modifier,
    )
}

/**
 * Tablero de postulaciones, siguiendo el diseño de Samuel.
 *
 * Las cuatro columnas van una al lado de la otra y se recorren de lado, como en el diseño.
 * En un teléfono no caben las cuatro a la vez, así que la fila lleva desplazamiento
 * horizontal y cada columna tiene un ancho fijo.
 */
@Composable
fun ApplicationsTracker(
    uiState: ApplicationsUiState,
    onStatusChange: (JobApplication, ApplicationStatus) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    // TODO: confirmar con Samuel a dónde llevan. No hay pantalla de detalle todavía.
    onDetailsClick: (JobApplication) -> Unit = {},
    onSeeAllClick: (ApplicationStatus) -> Unit = {},
) {
    Column(modifier = modifier.fillMaxSize()) {
        ApplicationsHeader(totalRecords = uiState.totalRecords)

        when {
            uiState.isLoading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            uiState.error != null -> ApplicationsError(
                message = uiState.error,
                onRetry = onRetry,
            )

            else -> Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(rememberScrollState())
                    .padding(12.dp),
            ) {
                ApplicationStatus.entries.forEach { status ->
                    StatusColumn(
                        status = status,
                        applications = uiState.applications.filter { it.status == status },
                        onStatusChange = onStatusChange,
                        onDetailsClick = onDetailsClick,
                        onSeeAllClick = { onSeeAllClick(status) },
                    )
                }
            }
        }
    }
}

/** Antetítulo, título y el contador total. */
@Composable
private fun ApplicationsHeader(
    totalRecords: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.applications_eyebrow),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.applications_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(50),
        ) {
            Text(
                text = stringResource(R.string.applications_count, totalRecords),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

/**
 * Una columna del tablero.
 *
 * Cada estado tiene su color, que pinta el borde superior, el punto del encabezado y el
 * botón del pie, como en el diseño.
 */
@Composable
private fun StatusColumn(
    status: ApplicationStatus,
    applications: List<JobApplication>,
    onStatusChange: (JobApplication, ApplicationStatus) -> Unit,
    onDetailsClick: (JobApplication) -> Unit,
    onSeeAllClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = status.accentColor()

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.width(COLUMN_WIDTH),
    ) {
        Column {
            // Franja de color en el borde de arriba.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(accent),
            )

            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(accent),
                    )
                    Text(
                        text = stringResource(status.labelRes()),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp),
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(50),
                    ) {
                        Text(
                            text = applications.size.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }

                applications.forEach { application ->
                    ApplicationCard(
                        application = application,
                        accent = accent,
                        onStatusChange = { newStatus -> onStatusChange(application, newStatus) },
                        onDetailsClick = { onDetailsClick(application) },
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }

                TextButton(
                    onClick = onSeeAllClick,
                    colors = ButtonDefaults.textButtonColors(contentColor = accent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                ) {
                    Text(
                        text = stringResource(R.string.applications_column_see_all),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}

/** Tarjeta de una postulación dentro de su columna. */
@Composable
private fun ApplicationCard(
    application: JobApplication,
    accent: Color,
    onStatusChange: (ApplicationStatus) -> Unit,
    onDetailsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.surfaceVariant,
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row {
                // Cuadrito con la inicial de la empresa, del color de la columna.
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = application.company.take(1).uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = accent,
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = application.jobTitle,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = application.company,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(
                            R.string.jobs_location_and_modality,
                            application.location,
                            stringResource(application.modality.labelRes()),
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                StatusMenu(
                    currentStatus = application.status,
                    onStatusChange = onStatusChange,
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 10.dp),
            ) {
                Text(
                    text = application.appliedAt.relativeLabel(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = onDetailsClick,
                    colors = ButtonDefaults.textButtonColors(contentColor = accent),
                    contentPadding = TIGHT_PADDING,
                ) {
                    Text(
                        text = stringResource(R.string.applications_action_details),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}

/**
 * Menú de los tres puntos: mueve la postulación a otra columna.
 *
 * Es la operación de actualización del CRUD contra el APIM.
 */
@Composable
private fun StatusMenu(
    currentStatus: ApplicationStatus,
    onStatusChange: (ApplicationStatus) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        TextButton(
            onClick = { expanded = true },
            contentPadding = TIGHT_PADDING,
        ) {
            Text(
                text = "⋮",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ApplicationStatus.entries.forEach { status ->
                DropdownMenuItem(
                    text = { Text(stringResource(status.labelRes())) },
                    enabled = status != currentStatus,
                    onClick = {
                        expanded = false
                        onStatusChange(status)
                    },
                )
            }
        }
    }
}

@Composable
private fun ApplicationsError(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.applications_error_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onRetry) {
                Text(stringResource(R.string.applications_retry))
            }
        }
    }
}

/** "Hoy", "Ayer" o "Hace N días", a partir de la fecha ISO del servicio. */
@Composable
private fun String.relativeLabel(): String {
    val days = daysSince() ?: return this
    return when (days) {
        0 -> stringResource(R.string.applications_applied_today)
        1 -> stringResource(R.string.applications_applied_yesterday)
        else -> pluralStringResource(R.plurals.applications_applied_days_ago, days, days)
    }
}

/** Color de cada columna, según el diseño. */
@Composable
private fun ApplicationStatus.accentColor(): Color = when (this) {
    ApplicationStatus.APPLIED -> MaterialTheme.colorScheme.primary
    ApplicationStatus.IN_PROGRESS -> MaterialTheme.colorScheme.secondary
    ApplicationStatus.INTERVIEW -> MaterialTheme.extraColors.onSuccessContainer
    ApplicationStatus.REJECTED -> MaterialTheme.colorScheme.error
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

/** Ancho de cada columna: entra una y se asoma la siguiente, que invita a desplazar. */
private val COLUMN_WIDTH = 280.dp
private val TIGHT_PADDING = androidx.compose.foundation.layout.PaddingValues(
    horizontal = 8.dp,
    vertical = 2.dp,
)

@Preview(name = "Tablero · claro", widthDp = 390, heightDp = 900)
@Composable
private fun ApplicationsPreviewLight() = ApplicationsPreview(darkTheme = false)

@Preview(name = "Tablero · oscuro", widthDp = 390, heightDp = 900)
@Composable
private fun ApplicationsPreviewDark() = ApplicationsPreview(darkTheme = true)

@Composable
private fun ApplicationsPreview(darkTheme: Boolean) {
    CvProTheme(darkTheme = darkTheme) {
        Surface(color = MaterialTheme.colorScheme.background) {
            ApplicationsTracker(
                uiState = ApplicationsUiState(
                    totalRecords = 3,
                    applications = listOf(
                        JobApplication(
                            id = "1",
                            jobTitle = "Data Analyst",
                            company = "TechCorp",
                            companyLogoUrl = null,
                            location = "San José",
                            modality = JobModality.HYBRID,
                            status = ApplicationStatus.APPLIED,
                            appliedAt = "2026-09-28T12:00:00Z",
                        ),
                        JobApplication(
                            id = "2",
                            jobTitle = "BI Developer",
                            company = "Nimbus",
                            companyLogoUrl = null,
                            location = "Heredia",
                            modality = JobModality.REMOTE,
                            status = ApplicationStatus.IN_PROGRESS,
                            appliedAt = "2026-09-26T12:00:00Z",
                        ),
                        JobApplication(
                            id = "3",
                            jobTitle = "Data Scientist Jr.",
                            company = "Bluewave",
                            companyLogoUrl = null,
                            location = "Santa Ana",
                            modality = JobModality.ON_SITE,
                            status = ApplicationStatus.INTERVIEW,
                            appliedAt = "2026-09-24T12:00:00Z",
                        ),
                    ),
                ),
                onStatusChange = { _, _ -> },
                onRetry = {},
            )
        }
    }
}
