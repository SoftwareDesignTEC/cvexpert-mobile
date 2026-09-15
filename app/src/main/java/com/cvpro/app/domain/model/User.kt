package com.cvpro.app.domain.model

/**
 * Usuario con sesión iniciada.
 *
 * Los nombres de campo (name, email, photoUrl) son los mismos que UserProfile en
 * docs/contract/models.ts, para mantener la paridad con la web. El resto del perfil
 * (ubicación, disponibilidad, sectores) llega desde el APIM en la tarea del CRUD.
 */
data class User(
    val name: String,
    val email: String,
    val photoUrl: String?,
)
