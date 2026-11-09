package com.cvpro.app.domain.model

/**
 * Título automático de un currículum: "Nombre – puesto más reciente".
 *
 * Antes el CV nacía con el nombre de la plantilla ("Executive") y así se quedaba en Mis
 * Currículums, aunque adentro dijera otra cosa. Ahora el título sigue al contenido mientras
 * el usuario no escriba uno propio.
 *
 * @param fallback qué poner en lugar del puesto cuando todavía no hay experiencias; se usa
 * el nombre de la plantilla.
 */
fun Cv.autoTitle(fallback: String): String =
    cvTitle(name = personalInfo.name, detail = latestPosition() ?: fallback)

/**
 * Título con que nace un CV creado desde una plantilla: todavía no tiene experiencias, así
 * que lleva el nombre de la persona y el de la plantilla ("Ana Mora – Executive").
 */
fun newCvTitle(personName: String, templateName: String): String =
    cvTitle(name = personName, detail = templateName)

/** "Nombre – detalle", o solo uno de los dos si el otro está vacío. */
private fun cvTitle(name: String, detail: String): String {
    val nombre = name.trim()
    val detalle = detail.trim()
    return when {
        nombre.isEmpty() -> detalle
        detalle.isEmpty() -> nombre
        else -> "$nombre$TITLE_SEPARATOR$detalle"
    }
}

/**
 * Si el título lo puso la app y no el usuario: está vacío, es el nombre de la plantilla (los
 * CV creados antes de este cambio) o coincide con el automático que le tocaba.
 */
fun Cv.hasAutoTitle(templateName: String): Boolean {
    val current = title.trim()
    return current.isEmpty() ||
        current == templateName.trim() ||
        current == autoTitle(templateName)
}

/**
 * Título que hay que guardar al editar.
 *
 * Si el usuario cambió el título en el editor, se respeta. Si no lo tocó y el que había era
 * automático, se recalcula con el contenido nuevo (por ejemplo, si agregó una experiencia).
 *
 * @param original el CV como estaba antes de abrir el editor.
 */
fun Cv.titleToSave(original: Cv, templateName: String): String =
    if (title == original.title && original.hasAutoTitle(templateName)) {
        autoTitle(templateName)
    } else {
        title
    }

/** Puesto del trabajo actual o, si no hay, del más reciente. Null si no hay ninguno. */
private fun Cv.latestPosition(): String? =
    workExperiences
        .filter { it.position.isNotBlank() }
        .maxWithOrNull(compareBy<WorkExperience>({ it.current }, { it.startDate ?: Long.MIN_VALUE }))
        ?.position
        ?.trim()

private const val TITLE_SEPARATOR = " – "
