package com.cvpro.app.data.remote.dto

import com.google.gson.JsonElement

/**
 * `AlertSettings` del contrato de Ofertas y Postulaciones (`GET` y `PUT /alerts`).
 *
 * Campos opcionales por la misma razón que en el resto de DTOs: Gson no respeta los tipos no
 * nulos de Kotlin. `updatedAt` es JsonElement porque llega en epoch (ver toEpochMillis).
 */
data class AlertSettingsDto(
    val enabled: Boolean? = null,
    val minMatch: Int? = null,
    val profile: MatchingProfileDto? = null,
    val updatedAt: JsonElement? = null,
)

/** `MatchingProfile`: los nombres son los del perfil, iguales en la web y la app. */
data class MatchingProfileDto(
    val skills: List<String>? = null,
    val location: String? = null,
    val modalities: List<String>? = null,
    val jobTypes: List<String>? = null,
    val sectors: List<String>? = null,
    val experienceYears: Int? = null,
)

/** `AlertSettingsInput`: lo que se manda al guardar. Los tres campos son obligatorios. */
data class AlertSettingsRequestDto(
    val enabled: Boolean,
    val minMatch: Int,
    val profile: MatchingProfileDto,
)
