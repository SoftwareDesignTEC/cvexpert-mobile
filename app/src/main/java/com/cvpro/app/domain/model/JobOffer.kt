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
    /** Jornada: tiempo completo, medio tiempo, freelance o práctica. */
    val type: String,
    val description: String,
    val requirements: List<String>,
    val salary: Salary?,
    /** Qué tanto encaja con el perfil, de 0 a 100. Lo calcula el servicio. */
    val matchScore: Int?,
    /** Fecha de publicación, ya lista para mostrar. */
    val postedAt: String,
)

/** Rango salarial de una oferta, con los mismos campos del contrato de la web. */
data class Salary(
    val min: Double,
    val max: Double,
    val currency: String,
)

/** Modalidad de trabajo, con los mismos valores del contrato de la web. */
enum class JobModality {
    REMOTE,
    ON_SITE,
    HYBRID,
}
