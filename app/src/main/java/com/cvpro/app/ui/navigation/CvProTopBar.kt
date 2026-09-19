package com.cvpro.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.cvpro.app.R

/**
 * Barra superior de la app.
 *
 * Del prototipo se conservan los tres accesos: el menú, la campana de notificaciones (lleva
 * al tablero de postulaciones) y la cuenta (lleva a Mi Perfil). El logo no se repite acá
 * porque en un teléfono no hay espacio: el título muestra en qué pantalla estás.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CvProTopBar(
    title: String,
    isDarkTheme: Boolean,
    onMenuClick: () -> Unit,
    onToggleThemeClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        title = { Text(text = title) },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = stringResource(R.string.nav_open_menu),
                )
            }
        },
        actions = {
            // Cambio rápido de tema. El ícono muestra a qué se va a cambiar: luna si ahora
            // está claro, sol si está oscuro. La opción completa vive en Configuración.
            IconButton(onClick = onToggleThemeClick) {
                Icon(
                    painter = painterResource(
                        if (isDarkTheme) R.drawable.ic_light_mode else R.drawable.ic_dark_mode,
                    ),
                    contentDescription = stringResource(
                        if (isDarkTheme) R.string.nav_theme_to_light else R.string.nav_theme_to_dark,
                    ),
                )
            }
            IconButton(onClick = onNotificationsClick) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = stringResource(R.string.nav_notifications),
                )
            }
            IconButton(onClick = onProfileClick) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = stringResource(R.string.nav_profile),
                )
            }
        },
        modifier = modifier,
    )
}
