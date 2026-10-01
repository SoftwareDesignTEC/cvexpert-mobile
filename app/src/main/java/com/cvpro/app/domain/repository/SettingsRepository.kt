package com.cvpro.app.domain.repository

import com.cvpro.app.domain.model.ThemePreference
import kotlinx.coroutines.flow.Flow

/** Preferencias de la app que sobreviven al cierre. */
interface SettingsRepository {

    /** Tema elegido. Emite de nuevo cada vez que cambia, así la app se repinta sola. */
    val themePreference: Flow<ThemePreference>

    suspend fun setThemePreference(preference: ThemePreference)

    /** Qué avisos quiere recibir el usuario. */
    val notifications: Flow<NotificationSettings>

    suspend fun setNotification(
        type: NotificationType,
        enabled: Boolean,
    )

    /** Indica si el usuario ya vio completa la animación inicial. */
    val hasSeenSplash: Flow<Boolean>

    suspend fun markSplashSeen()
}

/**
 * Los tres avisos que ofrece la app, según el diseño de Configuración.
 */
enum class NotificationType {
    APPLICATION_STATUS,
    COURSE_RECOMMENDATION,
    JOB_ALERTS,
}

/** Estado de los tres avisos. Todos empiezan encendidos. */
data class NotificationSettings(
    val applicationStatus: Boolean = true,
    val courseRecommendation: Boolean = true,
    val jobAlerts: Boolean = true,
) {
    fun isEnabled(type: NotificationType): Boolean = when (type) {
        NotificationType.APPLICATION_STATUS -> applicationStatus
        NotificationType.COURSE_RECOMMENDATION -> courseRecommendation
        NotificationType.JOB_ALERTS -> jobAlerts
    }
}