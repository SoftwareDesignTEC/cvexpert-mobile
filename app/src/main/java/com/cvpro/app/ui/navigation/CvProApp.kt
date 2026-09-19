package com.cvpro.app.ui.navigation

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cvpro.app.ui.components.LocalSnackbarHostState
import com.cvpro.app.ui.screens.login.LoginRoute
import com.cvpro.app.ui.theme.ThemeViewModel
import kotlinx.coroutines.launch

/**
 * Raíz de la app.
 *
 * Sin sesión muestra el login, y solo eso: no hay menú ni barra superior.
 * Con sesión arma el marco de la app (menú lateral + barra superior) y adentro el grafo
 * de navegación, que es el que cambia de pantalla.
 */
@Composable
fun CvProApp(
    modifier: Modifier = Modifier,
    sessionViewModel: SessionViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel(),
) {
    val currentUser by sessionViewModel.currentUser.collectAsStateWithLifecycle()
    val user = currentUser

    // Cómo se está viendo la app ahora mismo: la preferencia guardada, y si es "seguir al
    // sistema", lo que diga el teléfono. Lo usa el botón de la barra para saber qué ofrecer.
    val themePreference by themeViewModel.themePreference.collectAsStateWithLifecycle()
    val isDarkTheme = themePreference.isDark(isSystemInDarkTheme())

    if (user == null) {
        LoginRoute(modifier = modifier)
        return
    }

    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Destino actual: lo usan el título de la barra y el resaltado del menú.
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = CvProDestination.fromRoute(backStackEntry?.destination?.route)
        ?: CvProDestination.START

    val goTo: (CvProDestination) -> Unit = { destination ->
        navController.navigateToDestination(destination)
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            CvProDrawer(
                user = user,
                currentDestination = currentDestination,
                onDestinationClick = goTo,
                onSignOutClick = {
                    scope.launch { drawerState.close() }
                    sessionViewModel.signOut()
                },
            )
        },
        modifier = modifier,
    ) {
        // Un solo snackbar para toda la app: las pantallas lo usan desde LocalSnackbarHostState.
        val snackbarHostState = remember { SnackbarHostState() }

        Scaffold(
            topBar = {
                CvProTopBar(
                    title = stringResource(currentDestination.titleRes),
                    isDarkTheme = isDarkTheme,
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onToggleThemeClick = { themeViewModel.toggleTheme(isDarkTheme) },
                    onNotificationsClick = { goTo(CvProDestination.APPLICATIONS) },
                    onProfileClick = { goTo(CvProDestination.PROFILE) },
                )
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        ) { innerPadding ->
            CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
                CvProNavGraph(
                    navController = navController,
                    user = user,
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
    }
}
