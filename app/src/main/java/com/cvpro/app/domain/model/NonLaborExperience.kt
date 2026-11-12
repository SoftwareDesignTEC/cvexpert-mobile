package com.cvpro.app.domain.model

/** Lo que el usuario cuenta de una experiencia que no fue un empleo formal. */
data class NonLaborExperience(
    val type: NonLaborType,
    val organization: String,
    val position: String,
    val description: String,
)

/**
 * Tipos que acepta el traductor de IA (NonLaborExperienceType en el contrato).
 *
 * @property apiValue el valor del contrato, que también es el `type` que queda guardado en la
 *   experiencia del CV.
 */
enum class NonLaborType(val apiValue: String) {
    VOLUNTEER("volunteer"),
    SCHOOL_PROJECT("school-project"),
    INFORMAL("informal"),
}

/** Puesto y descripción ya redactados como en un CV. */
data class TranslatedExperience(
    val position: String,
    val description: String,
)
