package com.cvpro.app.ui.navigation

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cvpro.app.R

/**
 * Barra superior de la app, según el diseño de Samuel.
 *
 * De izquierda a derecha: el menú, el logo, el indicador de conexión, el cambio de tema, la
 * campana de notificaciones (lleva al rastreador de postulaciones) y la cuenta (lleva a Mi
 * Perfil). El nombre de la pantalla no va acá: cada pantalla lo muestra en su contenido,
 * como en los wireframes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CvProTopBar(
    isDarkTheme: Boolean,
    isOnline: Boolean,
    onMenuClick: () -> Unit,
    onToggleThemeClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppLogo()
                Spacer(modifier = Modifier.width(8.dp))
                OnlineBadge(isOnline = isOnline)
            }
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

/**
 * Logo de la app.
 *
 * Por ahora es el nombre dentro de un recuadro, igual que el marcador del wireframe: la
 * marca definitiva todavía está sin decidir.
 */
@Composable
private fun AppLogo(modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(6.dp),
        modifier = modifier,
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

/** Indicador de conexión: el "Estado Online/off" del diseño. */
@Composable
private fun OnlineBadge(
    isOnline: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = if (isOnline) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.errorContainer
        },
        shape = RoundedCornerShape(50),
        modifier = modifier,
    ) {
        Text(
            text = stringResource(
                if (isOnline) R.string.nav_status_online else R.string.nav_status_offline,
            ),
            style = MaterialTheme.typography.labelSmall,
            color = if (isOnline) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onErrorContainer
            },
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}
