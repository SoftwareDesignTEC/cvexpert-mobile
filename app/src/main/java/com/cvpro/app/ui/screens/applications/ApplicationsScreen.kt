package com.cvpro.app.ui.screens.applications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
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

    ApplicationsTracker(
        uiState = uiState,
        onStatusSelected = viewModel::onStatusSelected,
        onStatusChange = viewModel::onStatusChange,
        onRetry = viewModel::retry,
        modifier = modifier,
    )
}

/**
 * Rastreador de postulaciones, siguiendo el wireframe de Samuel.
 *
 * De arriba a abajo: el título, el contador alineado a la derecha, y una tarjeta blanca que
 * envuelve las cuatro pestañas de estado y la lista de postulaciones.
 *
 * @param onDetailsClick qué hacer al tocar "Detalles" de una tarjeta.
 * @param onSeeAllClick qué hacer al tocar "Ver Todo" al final de la lista.
 */
@Composable
fun ApplicationsTracker(
    uiState: ApplicationsUiState,
    onStatusSelected: (ApplicationStatus) -> Unit,
    onStatusChange: (JobApplication, ApplicationStatus) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    // TODO: confirmar con Samuel a dónde llevan. No hay pantalla de detalle todavía.
    onDetailsClick: (JobApplication) -> Unit = {},
    onSeeAllClick: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.applications_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )

        // El contador va alineado a la derecha, como en el wireframe.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.applications_count, uiState.totalRecords),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }

        // La tarjeta que contiene pestañas y lista. Ocupa el resto de la pantalla.
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            val error = uiState.error
            when {
                uiState.isLoading -> LoadingBox()

                error != null -> ErrorBox(message = error, onRetry = onRetry)

                else -> ApplicationsContent(
                    uiState = uiState,
                    onStatusSelected = onStatusSelected,
                    onStatusChange = onStatusChange,
                    onDetailsClick = onDetailsClick,
                    onSeeAllClick = onSeeAllClick,
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
private fun LoadingBox(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorBox(
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

/** Pestañas de estado y lista de postulaciones de la pestaña abierta. */
@Composable
private fun ApplicationsContent(
    uiState: ApplicationsUiState,
    onStatusSelected: (ApplicationStatus) -> Unit,
    onStatusChange: (JobApplication, ApplicationStatus) -> Unit,
    onDetailsClick: (JobApplication) -> Unit,
    onSeeAllClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        PrimaryScrollableTabRow(
            selectedTabIndex = ApplicationStatus.entries.indexOf(uiState.selectedStatus),
            edgePadding = 8.dp,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            ApplicationStatus.entries.forEach { status ->
                Tab(
                    selected = status == uiState.selectedStatus,
                    onClick = { onStatusSelected(status) },
                    // El wireframe muestra solo el nombre del estado, sin el número.
                    text = { Text(stringResource(status.labelRes())) },
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
                    ApplicationCard(
                        application = application,
                        onStatusChange = { newStatus -> onStatusChange(application, newStatus) },
                        onDetailsClick = { onDetailsClick(application) },
                    )
                }

                item {
                    Button(
                        onClick = onSeeAllClick,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.applications_see_all))
                    }
                }
            }
        }
    }
}

/**
 * Tarjeta de una postulación, con la forma del wireframe: el logo de la empresa a la
 * izquierda, los datos a la derecha, y abajo la fecha con los botones de acción.
 */
@Composable
private fun ApplicationCard(
    application: JobApplication,
    onStatusChange: (ApplicationStatus) -> Unit,
    onDetailsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row {
                CompanyLogo(
                    logoUrl = application.companyLogoUrl,
                    company = application.company,
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Cada dato en su propia línea, como los separa el wireframe.
                Column(modifier = Modifier.weight(1f)) {
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
                        text = application.location,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(application.modality.labelRes()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Los botones se miden primero y la fecha se queda con lo que sobra: sin el
                // weight, la fecha se lleva el ancho y las etiquetas de los botones quedan
                // partidas en vertical.
                Text(
                    text = stringResource(R.string.applications_applied_at, application.appliedAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusMenuButton(
                        currentStatus = application.status,
                        onStatusChange = onStatusChange,
                    )
                    OutlinedButton(
                        onClick = onDetailsClick,
                        contentPadding = COMPACT_BUTTON_PADDING,
                    ) {
                        Text(stringResource(R.string.applications_action_details))
                    }
                }
            }
        }
    }
}

/**
 * Botón "Estado": abre el menú con los cuatro estados y manda el cambio.
 *
 * Es la operación de actualización del CRUD contra el APIM.
 */
@Composable
private fun StatusMenuButton(
    currentStatus: ApplicationStatus,
    onStatusChange: (ApplicationStatus) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            contentPadding = COMPACT_BUTTON_PADDING,
        ) {
            Text(stringResource(R.string.applications_action_status))
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
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

/**
 * Recuadro del logo de la empresa.
 *
 * Si la oferta no trae logo (el contrato lo marca como opcional), se muestra la inicial de
 * la empresa para que la tarjeta no quede con un hueco.
 */
@Composable
private fun CompanyLogo(
    logoUrl: String?,
    company: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(64.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        if (logoUrl != null) {
            AsyncImage(
                model = logoUrl,
                contentDescription = stringResource(R.string.applications_logo_description, company),
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(4.dp),
            )
        } else {
            Text(
                text = company.take(1).uppercase(),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Relleno de los botones de la tarjeta.
 *
 * Más angosto que el de Material por defecto: "Estado" y "Detalles" tienen que caber en la
 * misma fila que la fecha en una pantalla de teléfono.
 */
private val COMPACT_BUTTON_PADDING = PaddingValues(horizontal = 12.dp, vertical = 6.dp)

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

/**
 * Vista previa con medidas de teléfono.
 *
 * Sin widthDp/heightDp el preview no le da tamaño a la pantalla y, como usa fillMaxSize,
 * se vería colapsada.
 */
@Preview(name = "Postulaciones · claro", widthDp = 390, heightDp = 844)
@Composable
private fun ApplicationsTrackerPreview() {
    ApplicationsTrackerPreviewContent(darkTheme = false)
}

/** La misma pantalla en oscuro, para comprobar que ningún color quedó fijo. */
@Preview(name = "Postulaciones · oscuro", widthDp = 390, heightDp = 844)
@Composable
private fun ApplicationsTrackerDarkPreview() {
    ApplicationsTrackerPreviewContent(darkTheme = true)
}

@Composable
private fun ApplicationsTrackerPreviewContent(darkTheme: Boolean) {
    CvProTheme(darkTheme = darkTheme) {
        Surface(color = MaterialTheme.colorScheme.background) {
            ApplicationsTracker(
                uiState = ApplicationsUiState(
                    applications = listOf(
                        JobApplication(
                            id = "1",
                            jobTitle = "Cajero de tienda",
                            company = "Automercado",
                            companyLogoUrl = null,
                            location = "Heredia",
                            modality = JobModality.ON_SITE,
                            status = ApplicationStatus.APPLIED,
                            appliedAt = "12/09/2026",
                        ),
                        JobApplication(
                            id = "2",
                            jobTitle = "Asistente administrativo",
                            company = "Grupo Monge",
                            companyLogoUrl = null,
                            location = "San José",
                            modality = JobModality.HYBRID,
                            status = ApplicationStatus.APPLIED,
                            appliedAt = "11/09/2026",
                        ),
                    ),
                    totalRecords = 2,
                ),
                onStatusSelected = {},
                onStatusChange = { _, _ -> },
                onRetry = {},
            )
        }
    }
}
