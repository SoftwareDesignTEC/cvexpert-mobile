package com.cvpro.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.cvpro.app.BuildConfig
import com.cvpro.app.domain.model.User
import com.cvpro.app.ui.screens.applications.ApplicationsScreen
import com.cvpro.app.ui.screens.cvs.CvsScreen
import com.cvpro.app.ui.screens.home.HomeScreen
import com.cvpro.app.ui.screens.jobs.JobsScreen
import com.cvpro.app.ui.screens.profile.ProfileScreen
import com.cvpro.app.ui.screens.settings.SettingsScreen
import com.cvpro.app.ui.screens.skillsgap.SkillsGapScreen
import com.cvpro.app.ui.screens.templates.TemplatesScreen

/**
 * Grafo de navegación: qué pantalla corresponde a cada destino del menú.
 *
 * El login queda afuera a propósito: sin sesión no se llega ni al menú ni a este grafo.
 */
@Composable
fun CvProNavGraph(
    navController: NavHostController,
    user: User,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = CvProDestination.START.route,
        modifier = modifier,
    ) {
        composable(CvProDestination.DASHBOARD.route) {
            HomeScreen(
                versionName = BuildConfig.VERSION_NAME,
                versionCode = BuildConfig.VERSION_CODE,
                // Accesos de cada sección del panel, como en el wireframe de Samuel.
                onTemplatesClick = { navController.navigateToDestination(CvProDestination.TEMPLATES) },
                onSkillsGapClick = { navController.navigateToDestination(CvProDestination.SKILLS_GAP) },
                onJobsClick = { navController.navigateToDestination(CvProDestination.JOBS) },
                onCvsClick = { navController.navigateToDestination(CvProDestination.CVS) },
            )
        }
        composable(CvProDestination.CVS.route) {
            CvsScreen(
                onTemplatesClick = { navController.navigateToDestination(CvProDestination.TEMPLATES) },
            )
        }
        composable(CvProDestination.PROFILE.route) {
            ProfileScreen(
                onSkillsGapClick = { navController.navigateToDestination(CvProDestination.SKILLS_GAP) },
            )
        }
        composable(CvProDestination.JOBS.route) { JobsScreen() }
        composable(CvProDestination.SKILLS_GAP.route) { SkillsGapScreen() }
        composable(CvProDestination.TEMPLATES.route) { TemplatesScreen() }
        composable(CvProDestination.APPLICATIONS.route) { ApplicationsScreen() }
        composable(CvProDestination.SETTINGS.route) { SettingsScreen() }
    }
}

/**
 * Abre un destino del menú.
 *
 * - `popUpTo(inicio) { saveState = true }`: no apila pantallas infinitas; deja el Panel
 *   Principal como base y guarda el estado de la que se deja atrás.
 * - `launchSingleTop`: si ya estás en ese destino, no lo abre otra vez.
 * - `restoreState`: al volver, la pantalla recupera su estado (por ejemplo, el scroll).
 */
fun NavHostController.navigateToDestination(destination: CvProDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
