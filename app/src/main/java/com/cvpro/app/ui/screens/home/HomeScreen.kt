package com.cvpro.app.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cvpro.app.R
import com.cvpro.app.domain.model.User
import com.cvpro.app.ui.components.VersionLabel
import com.cvpro.app.ui.theme.CvProTheme

/**
 * Pantalla principal provisional: saluda al usuario con sesión iniciada.
 *
 * Es un composable sin estado: recibe el usuario y la acción de cerrar sesión, sin conocer
 * ViewModels ni BuildConfig. En T2 se reemplaza por el Panel Principal del diseño.
 */
@Composable
fun HomeScreen(
    user: User,
    versionName: String,
    versionCode: Int,
    onSignOutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            // Deja libres las zonas del sistema (barra de estado y de gestos).
            .safeDrawingPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.home_greeting, user.name),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            text = user.email,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        OutlinedButton(
            onClick = onSignOutClick,
            modifier = Modifier.padding(top = 32.dp),
        ) {
            Text(text = stringResource(R.string.home_sign_out))
        }
        VersionLabel(
            versionName = versionName,
            versionCode = versionCode,
            modifier = Modifier.padding(top = 24.dp),
        )
    }
}

/** Envuelve los previews con el fondo del tema, para que el modo oscuro no salga sobre blanco. */
@Composable
private fun HomePreview(darkTheme: Boolean) {
    CvProTheme(darkTheme = darkTheme) {
        Surface(color = MaterialTheme.colorScheme.background) {
            HomeScreen(
                user = User(name = "María Rodríguez", email = "maria@ejemplo.com", photoUrl = null),
                versionName = "0.1.0",
                versionCode = 1,
                onSignOutClick = {},
            )
        }
    }
}

@Preview(name = "Claro")
@Composable
private fun HomeScreenPreviewLight() = HomePreview(darkTheme = false)

@Preview(name = "Oscuro")
@Composable
private fun HomeScreenPreviewDark() = HomePreview(darkTheme = true)
