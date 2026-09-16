package com.cvpro.app.ui.screens.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.cvpro.app.R
import com.cvpro.app.ui.components.PlaceholderScreen

/** Configuración: cuenta, notificaciones, tema e idioma. */
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = stringResource(R.string.destination_settings),
        description = stringResource(R.string.placeholder_settings),
        icon = Icons.Default.Settings,
        modifier = modifier,
    )
}
