package com.cvpro.app.data.remote.dto

import com.google.gson.JsonElement

/**
 * Cuerpo de `PUT /notifications/directory/me`.
 *
 * @property language "es" o "en", los dos valores que acepta el contrato de Notificaciones.
 */
data class DirectoryEntryRequestDto(
    val language: String,
)

/**
 * Esquema `Notification` del contrato de Notificaciones.
 *
 * `title` y `body` ya llegan escritos en el idioma del usuario (el servicio los arma con sus
 * plantillas), así que la app solo los muestra. `data` es el del evento original y no se usa
 * por ahora. Las fechas son JsonElement por lo mismo que en el resto de DTOs (ver
 * toEpochMillis), y todos los campos son opcionales porque Gson no respeta los no nulos.
 */
data class NotificationDto(
    val id: String? = null,
    val type: String? = null,
    val title: String? = null,
    val body: String? = null,
    val read: Boolean? = null,
    val createdAt: JsonElement? = null,
    val readAt: JsonElement? = null,
)

/** Respuesta de `GET /notifications/unread-count`. */
data class UnreadCountDto(
    val count: Int? = null,
)
