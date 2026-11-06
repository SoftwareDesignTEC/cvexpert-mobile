package com.cvpro.app.data.remote.api

import com.cvpro.app.data.remote.dto.DirectoryEntryRequestDto
import com.cvpro.app.data.remote.dto.NotificationDto
import com.cvpro.app.data.remote.dto.PagedResponseDto
import com.cvpro.app.data.remote.dto.UnreadCountDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Servicio de Notificaciones de Samuel, a través del APIM.
 *
 * Contrato: docs/contratos/openapi.yaml en cvexpert-ms-notificaciones (versión 0.1.0-borrador).
 * El usuario no se manda nunca: el APIM lo saca del token de Google ("me").
 */
interface NotificationApi {

    /**
     * Registra al usuario en el directorio de Notificaciones con su idioma ("es" o "en"), para
     * que los avisos le lleguen en ese idioma. Se llama al iniciar sesión y cuando cambia el
     * idioma del teléfono. La respuesta no trae nada que la app use, por eso Response<Unit>.
     */
    @PUT("notifications/directory/me")
    suspend fun updateDirectoryEntry(@Body body: DirectoryEntryRequestDto): Response<Unit>

    /**
     * Historial del usuario, más recientes primero.
     *
     * @param unread true solo no leídas, false solo leídas, null todas.
     */
    @GET("notifications")
    suspend fun getNotifications(
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int,
        @Query("unread") unread: Boolean?,
    ): PagedResponseDto<NotificationDto>

    /** Cuántas hay sin leer, para la insignia de la campana. */
    @GET("notifications/unread-count")
    suspend fun getUnreadCount(): UnreadCountDto

    /** Marca una como leída. Idempotente; 404 si no existe o no es del usuario. */
    @PATCH("notifications/{id}/read")
    suspend fun markRead(@Path("id") id: String): NotificationDto

    /** Marca todas como leídas. Responde 204 sin cuerpo. */
    @POST("notifications/read-all")
    suspend fun markAllRead(): Response<Unit>
}
