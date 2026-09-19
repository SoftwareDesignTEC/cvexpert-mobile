package com.cvpro.app.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector
import com.cvpro.app.R

/**
 * Destinos de la app, tal como aparecen en el menú lateral del prototipo.
 *
 * Están en un enum y no sueltos como textos para que el grafo, el menú y la barra superior
 * usen siempre la misma lista: si se agrega un destino, aparece solo en los tres lugares.
 *
 * @property route identificador del destino dentro del grafo.
 * @property titleRes nombre que se muestra en el menú y en la barra superior.
 */
enum class CvProDestination(
    val route: String,
    @param:StringRes val titleRes: Int,
    val icon: ImageVector,
) {
    DASHBOARD("panel", R.string.destination_dashboard, Icons.Default.Home),
    CVS("curriculums", R.string.destination_cvs, Icons.AutoMirrored.Filled.List),
    PROFILE("perfil", R.string.destination_profile, Icons.Default.Person),
    JOBS("ofertas", R.string.destination_jobs, Icons.Default.Search),
    SKILLS_GAP("brechas", R.string.destination_skills_gap, Icons.Default.Star),
    TEMPLATES("plantillas", R.string.destination_templates, Icons.Default.Create),
    APPLICATIONS("postulaciones", R.string.destination_applications, Icons.AutoMirrored.Filled.Send),
    SETTINGS("configuracion", R.string.destination_settings, Icons.Default.Settings),
    ;

    companion object {
        /** Destino que se muestra al entrar. */
        val START = DASHBOARD

        /** Busca el destino de una ruta; null si la ruta no es de esta lista. */
        fun fromRoute(route: String?): CvProDestination? = entries.firstOrNull { it.route == route }
    }
}
