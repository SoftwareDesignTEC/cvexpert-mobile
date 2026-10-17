package com.cvpro.app.domain.model

/**
 * Usuario con sesión iniciada.
 *
 * Los nombres de campo (name, email, photoUrl) son los mismos que UserProfile en
 * docs/contract/models.ts, para mantener la paridad con la web. El resto del perfil
 * (ubicación, disponibilidad, sectores) llega desde el APIM en la tarea del CRUD.
 */
data class User(
    /**
     * Identificador de la cuenta: el `sub` del token de Google. Es el id del perfil en el
     * servicio de Usuarios y el mismo que usa la web. Si el token no lo trae, se usa el
     * correo, que el servicio también acepta mientras dura la transición.
     */
    val id: String,
    val name: String,
    val email: String,
    val photoUrl: String?,
)
