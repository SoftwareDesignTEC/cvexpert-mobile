package com.cvpro.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cvpro.app.R
import com.cvpro.app.ui.theme.CvProTheme

/**
 * Los tres estados que debe manejar toda pantalla, además del de éxito: cargando, vacío y
 * error. Están acá para que se vean iguales en toda la app y no se reescriban en cada
 * pantalla.
 */

/** Cargando: ocupa la pantalla mientras se esperan datos. */
@Composable
fun LoadingState(
    modifier: Modifier = Modifier,
    message: String = stringResource(R.string.state_loading),
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

/**
 * Vacío: no hubo error, simplemente todavía no hay nada que mostrar.
 *
 * Si se pasa `actionText`, ofrece la acción que saca al usuario de ese estado, por ejemplo
 * "Crear mi primer currículum".
 */
@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.Info,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    InfoState(
        icon = icon,
        iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
        title = title,
        message = message,
        actionText = actionText,
        onActionClick = onActionClick,
        modifier = modifier,
    )
}

/**
 * Error: algo falló. Siempre muestra un mensaje entendible y el botón de reintentar, como
 * exige la rúbrica.
 */
@Composable
fun ErrorState(
    message: String,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.state_error_title),
) {
    InfoState(
        icon = Icons.Default.Warning,
        iconTint = MaterialTheme.colorScheme.error,
        title = title,
        message = message,
        actionText = stringResource(R.string.state_retry),
        onActionClick = onRetryClick,
        modifier = modifier,
    )
}

/** Base común de los estados vacío y error: ícono, título, mensaje y una acción opcional. */
@Composable
private fun InfoState(
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    title: String,
    message: String,
    actionText: String?,
    onActionClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = iconTint)
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        if (actionText != null && onActionClick != null) {
            SecondaryButton(
                text = actionText,
                onClick = onActionClick,
                modifier = Modifier
                    .padding(top = 24.dp)
                    .fillMaxWidth(fraction = 0.7f),
            )
        }
    }
}

@Preview(name = "Cargando")
@Composable
private fun LoadingStatePreview() {
    CvProTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) { LoadingState() }
    }
}

@Preview(name = "Vacío")
@Composable
private fun EmptyStatePreview() {
    CvProTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            EmptyState(
                title = "Todavía no tenés currículums",
                message = "Creá el primero y aparecerá acá.",
                actionText = "Crear currículum",
                onActionClick = {},
            )
        }
    }
}

@Preview(name = "Error - oscuro")
@Composable
private fun ErrorStatePreview() {
    CvProTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            ErrorState(message = "No pudimos cargar las ofertas.", onRetryClick = {})
        }
    }
}
