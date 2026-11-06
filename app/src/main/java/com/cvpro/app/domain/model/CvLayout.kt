package com.cvpro.app.domain.model

/**
 * Cómo se ve un currículum en el PDF: colores, tamaños de letra y orden de las secciones.
 *
 * El generador del PDF solo sabe dibujar; lo que cambia de una plantilla a otra vive acá. Hoy
 * hay un solo diseño ([DEFAULT]). Cuando el servicio de CVs de Pérez mande la definición
 * estructurada de cada plantilla, un mapper la convierte en un CvLayout y el generador no
 * cambia. Las plantillas preinstaladas para el modo sin conexión usarían el mismo formato.
 *
 * Los colores van como ARGB (0xFFRRGGBB): son del PDF, no de la pantalla, así que no salen
 * del tema de la app.
 *
 * @property sections qué secciones se dibujan y en qué orden. Una sección sin datos se omite.
 */
data class CvLayout(
    val accentColor: Int,
    val textColor: Int,
    val mutedColor: Int,
    val dividerColor: Int,
    val nameSize: Float,
    val titleSize: Float,
    val sectionSize: Float,
    val headingSize: Float,
    val bodySize: Float,
    val smallSize: Float,
    val sections: List<CvSection>,
) {
    companion object {
        /** El diseño que tenía el PDF antes de las plantillas, sin cambios. */
        val DEFAULT = CvLayout(
            accentColor = 0xFF2563EB.toInt(),
            textColor = 0xFF000000.toInt(),
            mutedColor = 0xFF64748B.toInt(),
            dividerColor = 0xFFCBD5E1.toInt(),
            nameSize = 22f,
            titleSize = 13f,
            sectionSize = 11f,
            headingSize = 11f,
            bodySize = 10f,
            smallSize = 9f,
            sections = listOf(CvSection.EXPERIENCE, CvSection.EDUCATION, CvSection.SKILLS, CvSection.LANGUAGES),
        )
    }
}

/** Secciones del currículum que puede ordenar una plantilla. El encabezado va siempre primero. */
enum class CvSection {
    EXPERIENCE,
    EDUCATION,
    SKILLS,
    LANGUAGES,
}
