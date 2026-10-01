package com.cvpro.app.data.mapper

import com.cvpro.app.data.remote.dto.UserProfileDto
import com.cvpro.app.domain.model.JobModality
import com.cvpro.app.domain.model.UserProfile

/**
 * Pasa el perfil de la API al modelo que usan las pantallas.
 *
 * Acá se traducen los nombres: el servicio manda `fullName` y `address`; el dominio usa
 * `name` y `location`, que son los del contrato de la web.
 *
 * Cada campo ausente se reemplaza por un valor vacío: el perfil se muestra incompleto en
 * vez de hacer caer la pantalla, que es lo que pasaba cuando el mock dejaba de mandar uno.
 */
fun UserProfileDto.toDomain(): UserProfile {
    return UserProfile(
        id = id.orEmpty(),
        name = fullName.orEmpty(),
        email = email.orEmpty(),
        phone = phone,
        location = address.orEmpty(),
        photoUrl = photoUrl,
        bio = bio,
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
        // Sin dato o valor desconocido: se asume presencial en vez de reventar. Una sola
        // modalidad mal escrita no debería dejar la pantalla sin perfil.
        else -> JobModality.ON_SITE
    }
}
