package com.cvpro.app.data.mapper

import com.google.gson.JsonElement
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

/** Formato en que se muestran las fechas en la app. */
private val DISPLAY_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy")

/**
 * Pasa una fecha ISO 8601 del servicio al formato que se lee en pantalla.
 *
 * El contrato de la web manda las fechas como "2026-09-20T15:00:00Z"; mostrarlas así
 * quedaría ilegible, así que se convierten a "20/09/2026" en la zona horaria del teléfono.
 *
 * Si el texto no viene en ISO se devuelve igual, sin reventar: es preferible mostrar una
 * fecha rara a que la pantalla se caiga por un dato mal formado del mock.
 */
fun String?.toDisplayDate(): String = try {
    if (this == null) return ""
    Instant.parse(this)
        .atZone(ZoneId.systemDefault())
        .format(DISPLAY_DATE_FORMAT)
} catch (exception: DateTimeParseException) {
    // Texto que no es ISO: se muestra tal cual en vez de fallar.
    this.orEmpty()
}

/**
 * Pasa una fecha del servicio a epoch en milisegundos UTC, o null si no se puede leer.
 *
 * Acepta los dos formatos que hay hoy: un número en epoch milisegundos (servicios de la
 * Fase II) y un texto ISO 8601 como "2026-09-20T15:00:00Z" (mocks de la Fase I).
 */
fun JsonElement?.toEpochMillis(): Long? {
    if (this == null || !isJsonPrimitive) return null
    val value = asJsonPrimitive
    return try {
        when {
            value.isNumber -> value.asLong
            value.isString -> value.asString.let { it.toLongOrNull() ?: Instant.parse(it).toEpochMilli() }
            else -> null
        }
    } catch (exception: DateTimeParseException) {
        null
    } catch (exception: NumberFormatException) {
        null
    }
}

/**
 * Cuántos días pasaron desde una fecha ISO 8601, o null si el texto no se puede leer.
 *
 * Lo usa el tablero de postulaciones, que muestra "Hace 3 días" en vez de la fecha. Se
 * devuelve el número y no el texto a propósito: el texto se arma en la pantalla, que es
 * donde están los strings traducibles.
 */
fun String?.daysSince(): Int? = try {
    if (this == null) return null
    val date = Instant.parse(this).atZone(ZoneId.systemDefault()).toLocalDate()
    ChronoUnit.DAYS.between(date, LocalDate.now()).toInt().coerceAtLeast(0)
} catch (exception: DateTimeParseException) {
    null
}
