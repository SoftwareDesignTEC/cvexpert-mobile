package com.cvpro.app.data.remote.api

import com.cvpro.app.data.remote.dto.AlertSettingsDto
import com.cvpro.app.data.remote.dto.AlertSettingsRequestDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

/** Alertas de ofertas, en el servicio de Ofertas y Postulaciones (docs/openapi.yaml). */
interface AlertsApi {

    /** 404 con código ALERTS_NOT_FOUND si el usuario nunca las guardó. */
    @GET("alerts")
    suspend fun getAlerts(): AlertSettingsDto

    /** Reemplaza todo. 400 VALIDATION_ERROR si algún valor no es válido. */
    @PUT("alerts")
    suspend fun saveAlerts(@Body body: AlertSettingsRequestDto): AlertSettingsDto
}
