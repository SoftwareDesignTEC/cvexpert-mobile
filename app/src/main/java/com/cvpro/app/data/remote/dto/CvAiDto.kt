package com.cvpro.app.data.remote.dto

/*
 * Cuerpos de las rutas de IA del servicio de CVs (/cvs/ai/...), tal como están en
 * docs/openapi.yaml de cvexpert-ms-cvs. Las respuestas van con campos opcionales por la misma
 * razón que el resto de los DTO: Gson no respeta los tipos no nulos de Kotlin.
 */

data class ExpandExperienceRequestDto(
    val company: String,
    val position: String,
    val description: String,
    /** formal, freelance, volunteer, school-project o informal. */
    val type: String,
    /** "es" o "en": el idioma en que la IA tiene que redactar. */
    val language: String,
)

data class ExpandExperienceResponseDto(
    val description: String? = null,
)

data class TranslateExperienceRequestDto(
    /** volunteer, school-project o informal. */
    val type: String,
    val position: String,
    val organization: String?,
    val description: String,
    val language: String,
)

data class TranslateExperienceResponseDto(
    val position: String? = null,
    val description: String? = null,
)
