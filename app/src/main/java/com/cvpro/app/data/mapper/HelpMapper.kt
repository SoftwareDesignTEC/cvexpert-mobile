package com.cvpro.app.data.mapper

import com.cvpro.app.data.remote.dto.HelpContactDto
import com.cvpro.app.domain.model.HelpContact

/**
 * Pasa un contacto de la API al modelo que usa la pantalla.
 *
 * Si falta la etiqueta se cae de vuelta al correo: es preferible mostrar la dirección a
 * dejar un botón sin texto.
 */
fun HelpContactDto.toDomain(): HelpContact {
    return HelpContact(
        type = type.orEmpty(),
        label = label.orEmpty().ifBlank { email.orEmpty() },
        email = email.orEmpty(),
    )
}
