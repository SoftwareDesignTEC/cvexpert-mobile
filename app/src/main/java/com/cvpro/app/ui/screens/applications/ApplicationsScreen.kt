package com.cvpro.app.ui.screens.applications

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.cvpro.app.R
import com.cvpro.app.ui.components.PlaceholderScreen

/** Postulaciones: tablero Kanban con el estado de cada postulación. */
@Composable
fun ApplicationsScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = stringResource(R.string.destination_applications),
        description = stringResource(R.string.placeholder_applications),
        icon = Icons.AutoMirrored.Filled.Send,
        modifier = modifier,
    )
}
