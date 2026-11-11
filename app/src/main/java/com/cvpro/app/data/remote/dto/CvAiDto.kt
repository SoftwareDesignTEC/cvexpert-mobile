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

/** POST /cvs/ai/generate-text: escribe un texto suelto (perfil o descripción). No guarda nada. */
data class GenerateTextRequestDto(
    /** "profileBio" o "experienceDescription". */
    val field: String,
    /** Lo que ya se sabe (puesto, empresa, borrador...): la IA lo usa como contexto. */
    val context: String,
    val language: String,
)

data class GenerateTextResponseDto(
    val text: String? = null,
)

/**
 * POST /cvs/ai/generate-cv: arma y guarda un CV completo a partir de un texto libre.
 * El correo no va: el servicio lo toma de X-User-Email.
 */
data class GenerateCvRequestDto(
    val title: String,
    val templateId: String,
    val fullName: String,
    val phone: String?,
    val freeText: String,
    val language: String,
)
