package com.cvpro.app.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.cvpro.app.R

/**
 * Número de versión de la app. Requisito de la rúbrica: tiene que verse dentro de la app.
 *
 * Es un componente para que todas las pantallas lo muestren igual. Recibe los valores
 * como parámetros (no lee BuildConfig) para poder usarlo en los @Preview.
 */
@Composable
fun VersionLabel(
    versionName: String,
    versionCode: Int,
    modifier: Modifier = Modifier,
) {
    Text(
        text = stringResource(R.string.version_label, versionName, versionCode),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}
