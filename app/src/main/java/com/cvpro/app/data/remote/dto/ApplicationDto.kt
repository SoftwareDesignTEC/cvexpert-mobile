package com.cvpro.app.data.remote.dto

import com.google.gson.JsonElement

/**
 * Representa una postulación tal como llega desde la API.
 *
 * Este modelo pertenece a la capa remota:
 * refleja el contrato JSON de APIM, no necesariamente
 * lo que utiliza internamente la aplicación.
 *
 * Las fechas son JsonElement porque el servicio de Postulaciones las manda en epoch
 * milisegundos y los mocks de la Fase I como texto ISO; el mapper acepta las dos.
 */
data class JobApplicationDto(
    val id: String? = null,
    val userId: String? = null,
    val jobOfferId: String? = null,
    val jobTitle: String? = null,
    val company: String? = null,
    val companyLogoUrl: String? = null,
    val location: String? = null,
    val modality: String? = null,
    val status: String? = null,
    val appliedAt: JsonElement? = null,
    val updatedAt: JsonElement? = null,
    val notes: String? = null,
)

/**
 * Datos que se envían al crear una nueva postulación.
 *
 * No incluye id, appliedAt ni updatedAt porque esos campos
 * normalmente los genera el servidor.
 */
data class CreateApplicationRequestDto(
    val userId: String? = null,
    val jobOfferId: String? = null,
    val jobTitle: String? = null,
    val company: String? = null,
    val companyLogoUrl: String? = null,
    val location: String? = null,
    val modality: String? = null,
    val status: String? = null,
    /** Columna donde nace la postulación ("interested", "applied"...). Sin valor, "applied". */
    val initialStatus: String? = null,
    val notes: String? = null,
)

/**
 * Datos necesarios para cambiar únicamente
 * el estado de una postulación.
 */
data class UpdateApplicationStatusRequestDto(
    val status: String? = null,
)