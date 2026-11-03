package com.cvpro.app.domain.repository

import com.cvpro.app.domain.model.JobAlertSettings
import com.cvpro.app.domain.model.UserProfile

/** Alertas de ofertas del usuario con sesión. El usuario sale del token, no se pasa. */
interface AlertsRepository {

    /** Las alertas guardadas, o null si el usuario nunca las configuró. */
    suspend fun getAlerts(): JobAlertSettings?

    /** Guarda todo (reemplaza lo anterior) y devuelve lo que quedó en el servicio. */
    suspend fun saveAlerts(settings: JobAlertSettings): JobAlertSettings

    /**
     * Copia a las alertas la parte que sale del perfil (ubicación, modalidades, jornadas y
     * sectores). Si el usuario nunca configuró alertas no hace nada: no se crean solas.
     */
    suspend fun syncProfile(profile: UserProfile)
}
