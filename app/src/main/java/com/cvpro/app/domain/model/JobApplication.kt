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
    /** Logo de la empresa que muestra la tarjeta del diseño. Opcional: puede no venir. */
    val companyLogoUrl: String?,
    val location: String,
    val modality: JobModality,
    val status: ApplicationStatus,
    /** Fecha en que se postuló, en epoch milisegundos UTC. Null si el servicio no la mandó. */
    val appliedAt: Long?,
    /**
     * Oferta a la que corresponde. Ofertas Laborales lo usa para marcar las ofertas a las que
     * el usuario ya se postuló. Vacío si el servicio no lo mandó.
     */
    val jobOfferId: String = "",
)

/**
 * Columnas del tablero de postulaciones, en el orden en que avanza el proceso.
 *
 * Son los cuatro estados del contrato de la web.
 */
/**
 * Columnas del tablero. INTERESTED ("Me interesa") guarda una oferta antes de postularse; se
 * agregó en la Fase II y va primero porque es el paso anterior a aplicar.
 */
enum class ApplicationStatus {
    INTERESTED,
    APPLIED,
    IN_PROGRESS,
    INTERVIEW,
    REJECTED,
}
