package com.cvpro.app.ui.screens.templates

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.cvpro.app.R
import com.cvpro.app.ui.components.PlaceholderScreen

/** Plantillas: catálogo sin conexión y tienda de plantillas para descargar. */
@Composable
fun TemplatesScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = stringResource(R.string.destination_templates),
        description = stringResource(R.string.placeholder_templates),
        icon = Icons.Default.Create,
        modifier = modifier,
    )
}
