package com.cvpro.app.domain.model

/**
 * Postulación del usuario a una oferta.
 *
 * Copia el puesto y la empresa a propósito, igual que el contrato de la web: si la oferta se
 * borra, la postulación se sigue viendo.
 */
data class JobApplication(
    val id: String,
    val jobTitle: String,
    val company: String,
    val location: String,
    val modality: JobModality,
    val status: ApplicationStatus,
    /** Fecha en que se postuló, ya lista para mostrar. */
    val appliedAt: String,
)

/**
 * Columnas del tablero de postulaciones, en el orden en que avanza el proceso.
 *
 * Son los cuatro estados del contrato de la web.
 */
enum class ApplicationStatus {
    APPLIED,
    IN_PROGRESS,
    INTERVIEW,
    REJECTED,
}
