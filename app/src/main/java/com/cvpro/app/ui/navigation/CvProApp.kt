package com.cvpro.app.ui.navigation

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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
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