package com.cvpro.app.data.remote.dto

/**
 * Perfil tal como llega desde la API.
 *
 * Ojo con los nombres: el servicio manda `fullName` y `address`, no `name` ni `location`
 * como dice el contrato de la web. El mapper los traduce a los del dominio.
 *
 * Todos los campos son opcionales a propósito. Gson no respeta los tipos no nulos de
 * Kotlin: si el JSON no trae un campo lo deja en null igual, y la app revienta al armar el
 * modelo con un error que no dice dónde se originó.
 */
data class UserProfileDto(
    val id: String? = null,
    val fullName: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val photoUrl: String? = null,
    val bio: String? = null,
    val types: List<String>? = null,
    val modalities: List<String>? = null,
    val sectors: List<String>? = null,
)

data class UpdateProfileRequestDto(
    val fullName: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val photoUrl: String? = null,
    val bio: String? = null,
    val types: List<String>? = null,
    val modalities: List<String>? = null,
    val sectors: List<String>? = null,
)
