package com.cvpro.app.data.remote.api

import com.cvpro.app.data.remote.dto.DirectoryEntryRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.PUT

/**
 * Servicio de Notificaciones de Samuel, a través del APIM.
 *
 * El usuario no se manda en el cuerpo: el APIM lo saca del token de Google ("me").
 */
interface NotificationApi {

    /**
     * Registra al usuario en el directorio de Notificaciones con su idioma, para que los
     * avisos le lleguen en español o en inglés. Se llama al iniciar sesión y cuando cambia
     * el idioma del teléfono.
     *
     * Se pide Response<Unit> porque la respuesta no trae nada que la app use.
     * TODO: confirmar con Samuel la forma de la respuesta cuando lo despliegue.
     */
    @PUT("notifications/directory/me")
    suspend fun updateDirectoryEntry(@Body body: DirectoryEntryRequestDto): Response<Unit>
}
