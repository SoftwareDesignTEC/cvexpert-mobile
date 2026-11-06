package com.cvpro.app.domain.model

/**
 * Una notificación del centro de notificaciones.
 *
 * Se llama AppNotification y no Notification para no chocar con android.app.Notification.
 *
 * @property title ya en el idioma del usuario: lo arma el servicio de Notificaciones.
 * @property createdAt epoch milisegundos UTC; null si el servicio no la mandó.
 */
data class AppNotification(
    val id: String,
    val kind: NotificationKind,
    val title: String,
    val body: String,
    val read: Boolean,
    val createdAt: Long?,
)

/** De qué evento salió la notificación. Define el ícono que se muestra. */
enum class NotificationKind {
    APPLICATION_STATUS,
    NEW_JOB,
    CV_GENERATED,
    COURSE_RECOMMENDED,

    /** Un tipo que la app todavía no conoce: se muestra igual, con un ícono genérico. */
    OTHER,
}
