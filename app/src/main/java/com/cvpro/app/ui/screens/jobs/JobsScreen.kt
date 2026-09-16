package com.cvpro.app.ui.screens.jobs

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.cvpro.app.R
import com.cvpro.app.ui.components.PlaceholderScreen

/** Ofertas Laborales: buscador y lista de ofertas con su porcentaje de compatibilidad. */
@Composable
fun JobsScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = stringResource(R.string.destination_jobs),
        description = stringResource(R.string.placeholder_jobs),
        icon = Icons.Default.Search,
        modifier = modifier,
    )
}
