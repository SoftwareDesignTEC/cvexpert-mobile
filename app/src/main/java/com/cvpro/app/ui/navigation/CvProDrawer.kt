package com.cvpro.app.ui.navigation

import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cvpro.app.R
import com.cvpro.app.domain.model.User
import androidx.compose.foundation.layout.widthIn

/**
 * Menú lateral con los ocho destinos, según el diseño de Samuel.
 *
 * Arriba el logo con el lema, en medio los destinos —el abierto queda resaltado en azul— y
 * abajo el centro de ayuda. En la versión web el menú está siempre visible; en el teléfono
 * se abre con el botón de las tres líneas, pero el contenido es el mismo.
 */
@Composable
fun CvProDrawer(
    user: User,
    currentDestination: CvProDestination,
    onDestinationClick: (CvProDestination) -> Unit,
    onSignOutClick: () -> Unit,
    onHelpClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalDrawerSheet(
        modifier = modifier
            .fillMaxWidth(0.82f)
            .widthIn(max = 320.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 16.dp),
        ) {
            DrawerHeader()

            Spacer(modifier = Modifier.padding(top = 8.dp))

            CvProDestination.entries.forEach { destination ->
                val isSelected = destination == currentDestination
                NavigationDrawerItem(
                    label = { Text(stringResource(destination.titleRes)) },
                    icon = { DestinationIcon(destination = destination, isSelected = isSelected) },
                    selected = isSelected,
                    onClick = { onDestinationClick(destination) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                )
            }

            // El cierre de sesión no va acá porque el diseño no lo dibuja: vive en
            // Configuración, dentro del bloque de seguridad de la cuenta.

            // Nada de weight acá: la columna tiene scroll, así que su altura es infinita y
            // un hijo con weight se mediría contra esa altura y rompería en ejecución.
            Spacer(modifier = Modifier.padding(top = 24.dp))

            HelpCard(onClick = onHelpClick)
        }
    }
}

/**
 * Ícono de un destino del menú.
 *
 * El destino abierto lleva el ícono dentro de un cuadrito azul, como en el diseño; los
 * demás van sueltos sobre el fondo del menú.
 */
@Composable
private fun DestinationIcon(
    destination: CvProDestination,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    Color.Transparent
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = destination.icon,
            contentDescription = null,
            tint = if (isSelected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(20.dp),
        )
    }
}

/** Logo, nombre y lema de la app, como en el encabezado del diseño. */
@Composable
private fun DrawerHeader(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(start = 12.dp),
    ) {
        LogoTile()
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.nav_tagline),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Cuadrito azul con la inicial de la marca. */
@Composable
private fun LogoTile(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(42.dp),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(
                R.drawable.logo_pequeno
            ),
            contentDescription = stringResource(
                R.string.app_name
            ),
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/**
 * Centro de ayuda al pie del menú.
 *
 * Al tocarlo abre el correo ya dirigido a soporte. Antes era solo un rótulo: se veía como
 * algo tocable pero no respondía.
 */
@Composable
private fun HelpCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.onSurface),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "?",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.surface,
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = stringResource(R.string.nav_help_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.nav_help_subtitle),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
