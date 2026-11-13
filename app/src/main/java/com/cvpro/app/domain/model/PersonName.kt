package com.cvpro.app.domain.model

/**
 * Nombre de una persona para llenar un formulario, o "" si no hay uno de verdad.
 *
 * Cuando la cuenta de Google no tiene nombre, antes se usaba el correo en su lugar y terminaba
 * escrito en "Nombre completo" del perfil y de los currículums. Un correo nunca es un nombre:
 * mejor dejar el campo vacío para que la persona lo escriba. Sirve también para perfiles que ya
 * quedaron guardados con el correo como nombre.
 */
fun personName(name: String?): String {
    val trimmed = name?.trim().orEmpty()
    return if ('@' in trimmed) "" else trimmed
}
