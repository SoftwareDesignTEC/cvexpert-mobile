package com.cvpro.app.data.mapper

import com.cvpro.app.data.remote.dto.UserProfileDto
import com.cvpro.app.domain.model.JobModality
import com.cvpro.app.domain.model.UserProfile

/**
 * Pasa el perfil de la API al modelo que usan las pantallas.
 *
 * Cada campo ausente se reemplaza por un valor vacío: el perfil se muestra incompleto en
 * vez de hacer caer la pantalla, que es lo que pasaba cuando el mock dejaba de mandar uno.
 */
fun UserProfileDto.toDomain(): UserProfile {
    return UserProfile(
        id = id.orEmpty(),
        name = name.orEmpty(),
        email = email.orEmpty(),
        phone = phone,
        location = location.orEmpty(),
        photoUrl = photoUrl,
        bio = bio,
        birthDate = birthDate,
        types = types.orEmpty(),
        modalities = modalities.orEmpty().map { it.toJobModality() },
        sectors = sectors.orEmpty(),
    )
}

private fun String?.toJobModality(): JobModality {
    return when (this?.lowercase()) {
        "remote" -> JobModality.REMOTE
        "on-site" -> JobModality.ON_SITE
        "hybrid" -> JobModality.HYBRID
        // Sin dato o valor desconocido: se asume presencial en vez de reventar.
        else -> JobModality.ON_SITE
    }
}
