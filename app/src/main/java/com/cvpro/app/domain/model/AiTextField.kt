package com.cvpro.app.domain.model

/** Qué texto le pide la app a la IA con "Generar con IA" (POST /cvs/ai/generate-text). */
enum class AiTextField(val apiValue: String) {
    /** El resumen o perfil profesional del CV. */
    PROFILE_BIO("profileBio"),

    /** La descripción de una experiencia. */
    EXPERIENCE_DESCRIPTION("experienceDescription"),
}
