package com.cvpro.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cvpro.app.BuildConfig
import com.cvpro.app.ui.screens.home.HomeScreen
import com.cvpro.app.ui.screens.login.LoginRoute

/**
 * Raíz de la app: sin sesión muestra el login; con sesión, la pantalla principal.
 *
 * Es una puerta de sesión provisional. En T2 se reemplaza por el grafo de navegación
 * (Navigation Component), que va a mantener esta misma regla.
 */
@Composable
fun CvProApp(
    modifier: Modifier = Modifier,
    sessionViewModel: SessionViewModel = hiltViewModel(),
) {
    val currentUser by sessionViewModel.currentUser.collectAsStateWithLifecycle()
    val user = currentUser

    if (user == null) {
        LoginRoute(modifier = modifier)
    } else {
        HomeScreen(
            user = user,
            versionName = BuildConfig.VERSION_NAME,
            versionCode = BuildConfig.VERSION_CODE,
            onSignOutClick = sessionViewModel::signOut,
            modifier = modifier,
        )
    }
}
