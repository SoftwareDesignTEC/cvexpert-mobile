package com.cvpro.app.ui.screens.profile

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.cvpro.app.R
import com.cvpro.app.ui.components.PlaceholderScreen

/** Mi Perfil: datos personales, contacto y preferencias, con sus formularios de edición. */
@Composable
fun ProfileScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = stringResource(R.string.destination_profile),
        description = stringResource(R.string.placeholder_profile),
        icon = Icons.Default.Person,
        modifier = modifier,
    )
}
