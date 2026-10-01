package com.cvpro.app.data.remote.dto

/**
 * Representa una postulación tal como llega desde la API.
 *
 * Este modelo pertenece a la capa remota:
 * refleja el contrato JSON de APIM, no necesariamente
 * lo que utiliza internamente la aplicación.
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
    val appliedAt: String? = null,
    val updatedAt: String? = null,
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
    val notes: String? = null,
)

/**
 * Datos necesarios para cambiar únicamente
 * el estado de una postulación.
 */
data class UpdateApplicationStatusRequestDto(
    val status: String? = null,
)