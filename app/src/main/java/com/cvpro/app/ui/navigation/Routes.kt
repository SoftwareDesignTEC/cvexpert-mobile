package com.cvpro.app.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ViewKanban
import androidx.compose.material.icons.outlined.Work
import androidx.compose.ui.graphics.vector.ImageVector
import com.cvpro.app.R

/**
 * Destinos de la app, tal como aparecen en el menú lateral del diseño.
 *
 * Están en un enum y no sueltos como textos para que el grafo, el menú y la barra superior
 * usen siempre la misma lista: si se agrega un destino, aparece solo en los tres lugares.
 *
 * Los íconos son los de Material en su variante Outlined, que es la que usa el diseño de
 * Samuel. Cinco de ellos —carpeta, maletín, gráfico, documento y kanban— solo existen en
 * `material-icons-extended`, por eso esa dependencia está en el catálogo.
 *
 * @property route identificador del destino dentro del grafo.
 * @property titleRes nombre que se muestra en el menú.
 */
enum class CvProDestination(
    val route: String,
    @param:StringRes val titleRes: Int,
    val icon: ImageVector,
) {
    DASHBOARD("panel", R.string.destination_dashboard, Icons.Outlined.Home),
    CVS("curriculums", R.string.destination_cvs, Icons.Outlined.Folder),
    PROFILE("perfil", R.string.destination_profile, Icons.Outlined.Person),
    JOBS("ofertas", R.string.destination_jobs, Icons.Outlined.Work),
    SKILLS_GAP("brechas", R.string.destination_skills_gap, Icons.Outlined.Analytics),
    TEMPLATES("plantillas", R.string.destination_templates, Icons.Outlined.Description),
    APPLICATIONS("postulaciones", R.string.destination_applications, Icons.Outlined.ViewKanban),
    SETTINGS("configuracion", R.string.destination_settings, Icons.Outlined.Settings),
    ;

    companion object {
        /** Destino que se muestra al entrar. */
        val START = DASHBOARD

        /** Busca el destino de una ruta; null si la ruta no es de esta lista. */
        fun fromRoute(route: String?): CvProDestination? = entries.firstOrNull { it.route == route }
    }
}
