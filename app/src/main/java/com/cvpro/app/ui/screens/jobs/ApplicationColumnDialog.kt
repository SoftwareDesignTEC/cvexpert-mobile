package com.cvpro.app.ui.screens.jobs

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cvpro.app.R
import com.cvpro.app.domain.model.ApplicationStatus

/**
 * "¿Dónde la guardas?": al agregar una oferta a Mis Postulaciones se elige la columna del
 * tablero. Antes el botón la mandaba siempre a "Aplicado", aunque el usuario solo quisiera
 * guardarla para después.
 *
 * Cada opción lleva una línea que explica cuándo usarla, para que no haya que adivinar.
 *
 * @param hasCompanyPage si la oferta tiene página de la empresa: "Aplicado" avisa que se abre.
 */
@Composable
fun ApplicationColumnDialog(
    hasCompanyPage: Boolean,
    onColumnClick: (ApplicationStatus) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.jobs_column_title)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = stringResource(R.string.jobs_column_message),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                ApplicationStatus.entries.forEach { status ->
                    ColumnOption(
                        label = stringResource(status.label()),
                        hint = stringResource(status.hint(hasCompanyPage)),
                        onClick = { onColumnClick(status) },
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.jobs_column_cancel)) }
        },
    )
}

@Composable
private fun ColumnOption(
    label: String,
    hint: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        // El borde hace que cada opción se vea como un botón también en modo oscuro.
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@StringRes
private fun ApplicationStatus.label(): Int = when (this) {
    ApplicationStatus.INTERESTED -> R.string.application_status_interested
    ApplicationStatus.APPLIED -> R.string.application_status_applied
    ApplicationStatus.IN_PROGRESS -> R.string.application_status_in_progress
    ApplicationStatus.INTERVIEW -> R.string.application_status_interview
    ApplicationStatus.REJECTED -> R.string.application_status_rejected
}

@StringRes
private fun ApplicationStatus.hint(hasCompanyPage: Boolean): Int = when (this) {
    ApplicationStatus.INTERESTED -> R.string.jobs_column_interested_hint
    ApplicationStatus.APPLIED ->
        if (hasCompanyPage) R.string.jobs_column_applied_hint_external else R.string.jobs_column_applied_hint
    ApplicationStatus.IN_PROGRESS -> R.string.jobs_column_in_progress_hint
    ApplicationStatus.INTERVIEW -> R.string.jobs_column_interview_hint
    ApplicationStatus.REJECTED -> R.string.jobs_column_rejected_hint
}
