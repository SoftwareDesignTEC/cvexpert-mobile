package com.cvpro.app.domain.model

/**
 * Oferta laboral.
 *
 * Los nombres de los campos son los de JobOffer en docs/contract/models.ts, para mantener la
 * paridad con la web.
 *
 * TODO: confirmar con Pérez los campos que el diseño muestra y el contrato no tiene todavía
 * (logo de la empresa) y el formato de postedAt.
 */
data class JobOffer(
    val id: String,
    val title: String,
    val company: String,
    val location: String,
    val modality: JobModality,
    val sector: String,
    val requirements: List<String>,
    /** Qué tanto encaja con el perfil, de 0 a 100. Lo calcula el servicio. */
    val matchScore: Int?,
)

/** Modalidad de trabajo, con los mismos valores del contrato de la web. */
enum class JobModality {
    REMOTE,
    ON_SITE,
    HYBRID,
}
