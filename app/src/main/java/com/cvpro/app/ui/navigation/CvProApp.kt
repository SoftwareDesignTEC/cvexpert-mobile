package com.cvpro.app.ui.navigation

import android.content.Intent
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cvpro.app.R
import com.cvpro.app.ui.components.LocalSnackbarHostState
import com.cvpro.app.ui.screens.login.LoginRoute
import kotlinx.coroutines.launch

/**
 * Raíz de la aplicación autenticada.
 *
 * Maneja navegación, drawer, barra superior y snackbar global.
 */
@Composable
fun CvProApp(
    modifier: Modifier = Modifier,
    sessionViewModel: SessionViewModel = hiltViewModel(),
    helpViewModel: HelpViewModel = hiltViewModel(),
) {
    val currentUser by sessionViewModel.currentUser.collectAsStateWithLifecycle()
    val user = currentUser

    if (user == null) {
        LoginRoute(modifier = modifier)
        return
    }

    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val context = LocalContext.current
    val asuntoSoporte = stringResource(R.string.settings_help_email_subject)
    val supportEmail by helpViewModel.supportEmail.collectAsStateWithLifecycle()

    val backStackEntry by navController.currentBackStackEntryAsState()

    val currentDestination =
        CvProDestination.fromRoute(backStackEntry?.destination?.route)
            ?: CvProDestination.START

    val goTo: (CvProDestination) -> Unit = { destination ->
        navController.navigateToDestination(destination)

        scope.launch {
            drawerState.close()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            CvProDrawer(
                user = user,
                currentDestination = currentDestination,
                onDestinationClick = goTo,
                onSignOutClick = {
                    scope.launch {
                        drawerState.close()
                    }

                    sessionViewModel.signOut()
                },
                onHelpClick = {
                    scope.launch {
                        drawerState.close()
                    }

                    // Con la dirección de soporte el correo se abre ya dirigido; si el
                    // servicio no respondió, se abre vacío y el usuario elige destinatario.
                    val destino = supportEmail
                        ?.takeIf { it.isNotBlank() }
                        ?.let { "mailto:$it" }
                        ?: "mailto:"

                    val correo = Intent(
                        Intent.ACTION_SENDTO,
                        destino.toUri(),
                    ).apply {
                        putExtra(Intent.EXTRA_SUBJECT, asuntoSoporte)
                    }

                    // Sin app de correo instalada, startActivity lanza excepción y tumbaría
                    // la app. Mejor que no pase nada a que se cierre.
                    runCatching { context.startActivity(correo) }
                },
            )
        },
        modifier = modifier,
    ) {
        val snackbarHostState = remember {
            SnackbarHostState()
        }

        Scaffold(
            topBar = {
                CvProTopBar(
                    currentDestination = currentDestination,
                    onMenuClick = {
                        scope.launch {
                            drawerState.open()
                        }
                    },
                    onProfileClick = {
                        goTo(CvProDestination.PROFILE)
                    },
                )
            },
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                )
            },
        ) { innerPadding ->

            CompositionLocalProvider(
                LocalSnackbarHostState provides snackbarHostState
            ) {
                CvProNavGraph(
                    navController = navController,
                    user = user,
                    onSignOutClick = sessionViewModel::signOut,
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
    }
}