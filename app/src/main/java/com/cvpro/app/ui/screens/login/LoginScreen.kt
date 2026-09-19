package com.cvpro.app.ui.screens.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cvpro.app.BuildConfig
import com.cvpro.app.R
import com.cvpro.app.ui.components.PrimaryButton
import com.cvpro.app.ui.components.VersionLabel
import com.cvpro.app.ui.theme.CvProTheme

/**
 * Versión "con estado" de la pantalla: conecta el ViewModel con la UI.
 *
 * LocalContext dentro de la Activity es la propia Activity, que es lo que necesita
 * Credential Manager para mostrar el selector de cuentas.
 */
@Composable
fun LoginRoute(
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LoginScreen(
        uiState = uiState,
        versionName = BuildConfig.VERSION_NAME,
        versionCode = BuildConfig.VERSION_CODE,
        onSignInClick = { viewModel.signIn(context) },
        modifier = modifier,
    )
}

/**
 * Pantalla de inicio de sesión, sin estado: solo pinta lo que recibe.
 *
 * Maneja tres casos: listo para entrar, cargando (mientras el selector de Google está
 * abierto) y error con botón de reintentar.
 */
@Composable
fun LoginScreen(
    uiState: LoginUiState,
    versionName: String,
    versionCode: Int,
    onSignInClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
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
            text = stringResource(R.string.home_tagline),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )

        uiState.errorMessage?.let { message ->
            Text(
                text = stringResource(message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 32.dp),
            )
        }
        // El botón muestra la rueda mientras carga, así que no hace falta manejarlo acá.
        // Con error, el mismo botón funciona como "Reintentar".
        PrimaryButton(
            text = stringResource(
                if (uiState.errorMessage == null) R.string.login_button else R.string.login_retry,
            ),
            onClick = onSignInClick,
            isLoading = uiState.isLoading,
            modifier = Modifier.padding(top = if (uiState.errorMessage == null) 48.dp else 16.dp),
        )

        VersionLabel(
            versionName = versionName,
            versionCode = versionCode,
            modifier = Modifier.padding(top = 24.dp),
        )
    }
}

/** Envuelve los previews con el fondo del tema, para que el modo oscuro no salga sobre blanco. */
@Composable
private fun LoginPreview(darkTheme: Boolean, uiState: LoginUiState) {
    CvProTheme(darkTheme = darkTheme) {
        Surface(color = MaterialTheme.colorScheme.background) {
            LoginScreen(uiState, versionName = "0.1.0", versionCode = 1, onSignInClick = {})
        }
    }
}

@Preview(name = "Listo - claro")
@Composable
private fun LoginScreenIdlePreview() = LoginPreview(darkTheme = false, uiState = LoginUiState())

@Preview(name = "Cargando - oscuro")
@Composable
private fun LoginScreenLoadingPreview() = LoginPreview(darkTheme = true, uiState = LoginUiState(isLoading = true))

@Preview(name = "Error - claro")
@Composable
private fun LoginScreenErrorPreview() =
    LoginPreview(darkTheme = false, uiState = LoginUiState(errorMessage = R.string.login_error_generic))
