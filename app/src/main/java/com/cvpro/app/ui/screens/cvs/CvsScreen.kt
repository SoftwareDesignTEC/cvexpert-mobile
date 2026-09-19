package com.cvpro.app.ui.screens.cvs

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.cvpro.app.R
import com.cvpro.app.ui.components.PlaceholderScreen

/** Mis Currículums: lista de los CV del usuario, con vista previa, editar y descargar. */
@Composable
fun CvsScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = stringResource(R.string.destination_cvs),
        description = stringResource(R.string.placeholder_cvs),
        icon = Icons.AutoMirrored.Filled.List,
        modifier = modifier,
    )
}
