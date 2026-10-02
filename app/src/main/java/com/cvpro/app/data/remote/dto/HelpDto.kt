package com.cvpro.app.data.remote.dto

/**
 * Contactos del centro de ayuda tal como llegan desde la API.
 *
 * El servicio los entrega envueltos en un objeto con una sola lista:
 * `{ "contacts": [ { "type": ..., "label": ..., "email": ... } ] }`.
 *
 * Es el único endpoint que no viene paginado, así que no usa PagedResponseDto.
 *
 * Todos los campos son opcionales a propósito, como en el resto de los DTO: Gson no respeta
 * los tipos no nulos de Kotlin y un campo ausente haría caer la pantalla.
 */
data class HelpContactsResponseDto(
    val contacts: List<HelpContactDto>? = null,
)

data class HelpContactDto(
    val type: String? = null,
    val label: String? = null,
    val email: String? = null,
)
