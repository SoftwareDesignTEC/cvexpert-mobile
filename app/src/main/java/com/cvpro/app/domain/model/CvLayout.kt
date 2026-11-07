package com.cvpro.app.domain.model

/**
 * Cómo se ve un currículum en el PDF: colores, tamaños, columnas y qué se muestra de cada
 * sección.
 *
 * El generador del PDF solo sabe dibujar; lo que cambia de una plantilla a otra vive acá.
 * Sale de dos lados:
 * - [DEFAULT], el diseño de siempre, para las plantillas que todavía no traen definición
 *   (el servicio manda `layout: null`).
 * - La definición estructurada que manda el servicio de CVs de Pérez (`Template.layout`),
 *   que CvTemplateMapper convierte en un CvLayout.
 *
 * El layout dice QUÉ se muestra y en qué orden y columna, no la tipografía exacta: eso lo
 * decide cada cliente (así quedó acordado con la web). Los tamaños de letra son siempre los
 * de [DEFAULT].
 *
 * Los colores van como ARGB (0xFFRRGGBB): son del PDF, no de la pantalla, así que no salen
 * del tema de la app.
 *
 * @property columns 1 o 2. Con 2, el encabezado va a todo el ancho y cada sección cae en la
 *   columna que diga su [CvLayoutSection.column].
 * @property header qué datos personales lleva el encabezado. Vacío = sin encabezado.
 * @property showCvTitle si se escribe el título del CV debajo del nombre. Solo lo hace el
 *   diseño de siempre: el contrato de plantillas no lo incluye.
 * @property sections qué secciones se dibujan y en qué orden. Una sección sin datos se omite.
 */
data class CvLayout(
    val accentColor: Int,
    val textColor: Int,
    val mutedColor: Int,
    val dividerColor: Int,
    val nameSize: Float = 22f,
    val titleSize: Float = 13f,
    val sectionSize: Float = 11f,
    val headingSize: Float = 11f,
    val bodySize: Float = 10f,
    val smallSize: Float = 9f,
    val columns: Int = 1,
    val header: List<HeaderField>,
    val showCvTitle: Boolean = false,
    val sections: List<CvLayoutSection>,
) {
    companion object {
        /** El diseño que tenía el PDF antes de las plantillas, sin cambios. */
        val DEFAULT = CvLayout(
            accentColor = 0xFF2563EB.toInt(),
            textColor = 0xFF000000.toInt(),
            mutedColor = 0xFF64748B.toInt(),
            dividerColor = 0xFFCBD5E1.toInt(),
            header = listOf(HeaderField.FULL_NAME, HeaderField.EMAIL, HeaderField.PHONE, HeaderField.ADDRESS, HeaderField.BIO),
            showCvTitle = true,
            // Todas las secciones con todos sus campos, en el orden del enum. Las certificaciones
            // son nuevas y van últimas, así que lo que ya había no se mueve. El tipo de
            // experiencia (voluntariado, proyecto escolar…) no se mostraba antes y sigue sin
            // mostrarse acá; una plantilla lo puede pedir.
            sections = CvSection.entries.map { CvLayoutSection(source = it, fields = it.allFields - "type") },
        )
    }
}

/**
 * Una sección del currículum dentro de una plantilla.
 *
 * @property fields qué campos de cada elemento se muestran, en orden. Son los nombres del
 *   contrato ("position", "company"…); los válidos para cada sección están en [CvSection.allFields].
 * @property column 1 o 2. Solo cuenta si el layout tiene 2 columnas.
 */
data class CvLayoutSection(
    val source: CvSection,
    val fields: List<String>,
    val column: Int = 1,
)

/**
 * Secciones que puede tener una plantilla. Cada una apunta a una lista del CV.
 *
 * @property apiName el valor de `source` en el contrato de plantillas.
 * @property allFields los `itemFields` que el contrato permite para esa sección, en el orden
 *   en que los muestra el diseño de siempre.
 */
enum class CvSection(val apiName: String, val allFields: List<String>) {
    EXPERIENCE("workExperiences", listOf("position", "company", "startDate", "endDate", "current", "description", "type")),
    EDUCATION("education", listOf("degree", "institution", "field", "startDate", "endDate", "current")),
    SKILLS("skills", listOf("name", "level", "category")),
    LANGUAGES("languages", listOf("name", "level")),
    CERTIFICATIONS("certifications", listOf("name", "issuer", "issuedAt", "expiresAt", "credentialUrl")),
}

/**
 * Datos personales que puede llevar el encabezado.
 *
 * @property apiName la ruta del contrato (`header.fields`).
 */
enum class HeaderField(val apiName: String) {
    FULL_NAME("personalInfo.fullName"),
    EMAIL("personalInfo.email"),
    PHONE("personalInfo.phone"),
    ADDRESS("personalInfo.address"),
    BIO("personalInfo.bio"),
    // El contrato también permite personalInfo.photoUrl y personalInfo.birthDate. No están acá
    // a propósito: la foto habría que descargarla antes de dibujar el PDF y el perfil de la app
    // no guarda fecha de nacimiento. Por la regla de "lo desconocido se ignora", el mapper las
    // salta sin romper.
}
