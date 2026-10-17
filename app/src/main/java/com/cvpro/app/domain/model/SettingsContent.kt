package com.cvpro.app.domain.model

/*
 * Las fechas son epoch en milisegundos UTC, como en el contrato. La pantalla las muestra en
 * la zona horaria del teléfono. null = el servicio no mandó una fecha que se pueda leer.
 */
data class ActiveSession(
    val id: String,
    val deviceName: String,
    val platform: String,
    val client: String,
    val lastActiveAt: Long?,
    val current: Boolean,
)

data class LegalDocument(
    val id: String,
    val title: String,
    val version: String,
    val effectiveDate: Long?,
    val content: String,
)