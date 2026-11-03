package com.cvpro.app.data.mapper

import com.cvpro.app.data.remote.dto.AlertSettingsDto
import com.cvpro.app.data.remote.dto.AlertSettingsRequestDto
import com.cvpro.app.data.remote.dto.MatchingProfileDto
import com.cvpro.app.domain.model.JobAlertSettings
import com.cvpro.app.domain.model.JobModality

fun AlertSettingsDto.toDomain(): JobAlertSettings {
    return JobAlertSettings(
        enabled = enabled ?: false,
        minMatch = minMatch ?: JobAlertSettings.DEFAULT_MIN_MATCH,
        skills = profile?.skills.orEmpty(),
        experienceYears = profile?.experienceYears ?: 0,
        location = profile?.location.orEmpty(),
        // Una modalidad que no se conoce se descarta en vez de convertirla en presencial:
        // acá sí importa, porque cambia con qué ofertas se empareja.
        modalities = profile?.modalities.orEmpty().mapNotNull { it.toJobModalityOrNull() },
        jobTypes = profile?.jobTypes.orEmpty(),
        sectors = profile?.sectors.orEmpty(),
        updatedAt = updatedAt.toEpochMillis(),
    )
}

fun JobAlertSettings.toRequest(): AlertSettingsRequestDto {
    return AlertSettingsRequestDto(
        enabled = enabled,
        minMatch = minMatch.coerceIn(0, 100),
        profile = MatchingProfileDto(
            skills = skills,
            location = location.takeIf { it.isNotBlank() },
            modalities = modalities.map { it.toWireValue() },
            // El servicio responde 400 si llega una jornada que no conoce. Si el perfil trae
            // una rara (por ejemplo, de un mock viejo), mejor no mandarla que no poder guardar.
            jobTypes = jobTypes.filter { it in JobAlertSettings.KNOWN_JOB_TYPES },
            sectors = sectors,
            experienceYears = experienceYears,
        ),
    )
}

private fun String.toJobModalityOrNull(): JobModality? = when (lowercase()) {
    "remote" -> JobModality.REMOTE
    "on-site" -> JobModality.ON_SITE
    "hybrid" -> JobModality.HYBRID
    else -> null
}

private fun JobModality.toWireValue(): String = when (this) {
    JobModality.REMOTE -> "remote"
    JobModality.ON_SITE -> "on-site"
    JobModality.HYBRID -> "hybrid"
}
