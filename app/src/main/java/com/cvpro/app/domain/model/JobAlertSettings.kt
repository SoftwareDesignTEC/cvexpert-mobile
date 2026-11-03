package com.cvpro.app.domain.model

/**
 * Alertas de ofertas nuevas del usuario y el perfil con el que se calcula la compatibilidad.
 *
 * Se guarda en el servicio de Ofertas (`/alerts`), no en el teléfono: cuando alguien publica
 * una oferta, el servicio la compara con este perfil y, si llega al mínimo, avisa por
 * Notificaciones. El mismo perfil es el que da el `matchScore` de la lista de ofertas.
 *
 * Una parte la escribe el usuario en la pantalla de alertas (habilidades y años de
 * experiencia, que el perfil de Usuarios no tiene) y otra se copia del perfil (ubicación,
 * modalidades, jornadas y sectores).
 *
 * @property minMatch compatibilidad mínima, de 0 a 100, para recibir el aviso.
 * @property jobTypes jornadas aceptadas, con los valores del contrato ("full-time", ...).
 * @property updatedAt último cambio en epoch milisegundos; null si nunca se guardó.
 */
data class JobAlertSettings(
    val enabled: Boolean,
    val minMatch: Int,
    val skills: List<String>,
    val experienceYears: Int,
    val location: String,
    val modalities: List<JobModality>,
    val jobTypes: List<String>,
    val sectors: List<String>,
    val updatedAt: Long?,
) {
    companion object {
        /** Mínimo con el que arranca quien nunca guardó alertas. */
        const val DEFAULT_MIN_MATCH = 70

        /** Jornadas que acepta el contrato. Cualquier otro valor el servicio lo rechaza. */
        val KNOWN_JOB_TYPES = listOf("full-time", "part-time", "freelance", "internship")
    }
}
