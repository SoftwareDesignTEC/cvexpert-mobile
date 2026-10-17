package com.cvpro.app.data.remote.dto

import com.google.gson.JsonElement

/*
 * lastActiveAt y effectiveDate llegan en epoch milisegundos UTC desde el servicio de
 * Usuarios de la Fase II, pero los mocks de la Fase I los mandan como texto ISO 8601. Se
 * reciben como JsonElement para aceptar los dos mientras dura la transición; el mapper los
 * convierte a Long (ver toEpochMillis).
 */
data class ActiveSessionDto(
    val id: String,
    val deviceName: String,
    val platform: String,
    val client: String,
    val lastActiveAt: JsonElement? = null,
    val current: Boolean,
)

data class LegalDocumentDto(
    val id: String,
    val title: String,
    val version: String,
    val effectiveDate: JsonElement? = null,
    val content: String,
)