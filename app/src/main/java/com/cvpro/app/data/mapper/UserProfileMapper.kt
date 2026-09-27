package com.cvpro.app.data.mapper

import com.cvpro.app.data.remote.dto.UserProfileDto
import com.cvpro.app.domain.model.JobModality
import com.cvpro.app.domain.model.UserProfile

fun UserProfileDto.toDomain(): UserProfile {
    return UserProfile(
        id = id,
        name = name,
        email = email,
        phone = phone,
        location = location,
        photoUrl = photoUrl,
        bio = bio,
        birthDate = birthDate,
        types = types,
        modalities = modalities.map { it.toJobModality() },
        sectors = sectors,
    )
}

private fun String.toJobModality(): JobModality {
    return when (lowercase()) {
        "remote" -> JobModality.REMOTE
        "on-site" -> JobModality.ON_SITE
        "hybrid" -> JobModality.HYBRID
        else -> throw IllegalArgumentException("Modalidad desconocida: $this")
    }
}