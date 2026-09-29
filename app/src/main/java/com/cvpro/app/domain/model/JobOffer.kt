package com.cvpro.app.domain.model

/**
 * Oferta laboral.
 *
 * Los nombres de los campos son los de JobOffer en docs/contract/models.ts, para mantener la
 * paridad con la web.
 */
data class JobOffer(
    val id: String,
    val title: String,
    val company: String,
    /** Logo de la empresa que muestra la tarjeta del diseño. Opcional: puede no venir. */
    val companyLogoUrl: String?,
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
