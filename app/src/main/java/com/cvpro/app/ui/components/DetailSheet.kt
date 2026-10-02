package com.cvpro.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cvpro.app.R
import com.cvpro.app.domain.model.ApplicationStatus
import com.cvpro.app.domain.model.JobApplication
import com.cvpro.app.domain.model.JobModality
import com.cvpro.app.domain.model.JobOffer

/**
 * Detalle de una oferta laboral, en una hoja que sube desde abajo.
 *
 * Se usa una hoja y no una pantalla nueva porque el detalle se abre desde tres lugares
 * distintos (el panel, las ofertas y las postulaciones) y así no hay que pasar el
 * identificador por el grafo de navegación ni volver a pedirle el dato al servicio.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobOfferDetailSheet(
    offer: JobOffer,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                text = offer.title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = offer.company,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 12.dp),
            ) {
                DetailChip(text = offer.location)
                DetailChip(text = stringResource(offer.modality.labelRes()))
            }
            if (offer.sector.isNotBlank() || offer.type.isNotBlank()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 6.dp),
                ) {
                    DetailChip(text = offer.sector)
                    DetailChip(text = offer.type)
                }
            }

            offer.matchScore?.let { score ->
                DetailField(
                    label = stringResource(R.string.jobs_match_label),
                    value = stringResource(R.string.jobs_match_percentage, score),
                )
            }

            offer.salary?.let { salary ->
                DetailField(
                    label = stringResource(R.string.detail_salary),
                    value = stringResource(
                        R.string.detail_salary_range,
                        salary.min.toLong(),
                        salary.max.toLong(),
                        salary.currency,
                    ),
                )
            }

            if (offer.description.isNotBlank()) {
                DetailField(
                    label = stringResource(R.string.detail_description),
                    value = offer.description,
                )
            }

            if (offer.requirements.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.jobs_requirements_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 16.dp),
                )
                offer.requirements.forEach { requirement ->
                    Text(
                        text = stringResource(R.string.jobs_requirement_bullet, requirement),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            DetailField(
                label = stringResource(R.string.detail_posted_at),
                value = offer.postedAt,
            )
        }
    }
}

/**
 * Detalle de una postulación.
 *
 * Muestra lo que la tarjeta del tablero no alcanza a mostrar, y desde acá también se puede
 * cambiar de columna sin volver al menú de los tres puntos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobApplicationDetailSheet(
    application: JobApplication,
    onDismiss: () -> Unit,
    onStatusChange: (ApplicationStatus) -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                text = application.jobTitle,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = application.company,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 12.dp),
            ) {
                DetailChip(text = application.location)
                DetailChip(text = stringResource(application.modality.labelRes()))
            }

            DetailField(
                label = stringResource(R.string.detail_applied_at),
                value = application.appliedAt,
            )

            // Cambiar de columna desde el detalle: es la misma operación del menú de la
            // tarjeta, pero acá se ven las cuatro opciones a la vez.
            Text(
                text = stringResource(R.string.applications_change_status),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 20.dp),
            )
            ApplicationStatus.entries.forEach { status ->
                StatusOptionRow(
                    label = stringResource(status.labelRes()),
                    isCurrent = status == application.status,
                    onClick = { onStatusChange(status) },
                )
            }
        }
    }
}

/** Una opción de estado dentro del detalle de la postulación. */
@Composable
private fun StatusOptionRow(
    label: String,
    isCurrent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = if (isCurrent) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        shape = RoundedCornerShape(8.dp),
        onClick = onClick,
        enabled = !isCurrent,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isCurrent) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.weight(1f),
            )
            if (isCurrent) {
                Text(
                    text = stringResource(R.string.detail_current_status),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/** Etiqueta redonda con un dato corto. */
@Composable
private fun DetailChip(
    text: String,
    modifier: Modifier = Modifier,
) {
    // Sin texto no se dibuja nada. Cuando el servicio no manda sector o jornada, la etiqueta
    // quedaba como un círculo azul vacío al pie del detalle, que no significa nada para el
    // usuario. Mejor que falte la etiqueta a que aparezca una vacía.
    if (text.isBlank()) return

    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(50),
        modifier = modifier,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

/** Un dato con su etiqueta arriba. */
@Composable
private fun DetailField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(top = 16.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun JobModality.labelRes(): Int = when (this) {
    JobModality.REMOTE -> R.string.modality_remote
    JobModality.ON_SITE -> R.string.modality_on_site
    JobModality.HYBRID -> R.string.modality_hybrid
}

private fun ApplicationStatus.labelRes(): Int = when (this) {
    ApplicationStatus.APPLIED -> R.string.application_status_applied
    ApplicationStatus.IN_PROGRESS -> R.string.application_status_in_progress
    ApplicationStatus.INTERVIEW -> R.string.application_status_interview
    ApplicationStatus.REJECTED -> R.string.application_status_rejected
}
