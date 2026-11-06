package com.cvpro.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.cvpro.app.BuildConfig
import com.cvpro.app.R

/**
 * Barra superior principal de la app.
 *
 * Muestra únicamente navegación y acciones frecuentes:
 * menú lateral, nombre de la pantalla actual y perfil.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CvProTopBar(
    currentDestination: CvProDestination,
    onMenuClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
    unreadNotifications: Int = 0,
    onNotificationsClick: () -> Unit = {},
) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(currentDestination.titleRes),
            )
        },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = stringResource(R.string.nav_open_menu),
                )
            }
        },
        actions = {
            // La campana aparece solo cuando el servicio de Notificaciones está encendido.
            if (BuildConfig.NOTIFICATIONS_ENABLED) {
                IconButton(onClick = onNotificationsClick) {
                    BadgedBox(
                        badge = {
                            if (unreadNotifications > 0) {
                                // Más de 99 no entra en la insignia.
                                Badge { Text(if (unreadNotifications > 99) "99+" else unreadNotifications.toString()) }
                            }
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = stringResource(R.string.nav_notifications),
                        )
                    }
                }
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