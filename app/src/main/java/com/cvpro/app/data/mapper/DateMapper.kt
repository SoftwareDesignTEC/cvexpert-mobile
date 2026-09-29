package com.cvpro.app.data.mapper

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

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
fun String.toDisplayDate(): String = try {
    Instant.parse(this)
        .atZone(ZoneId.systemDefault())
        .format(DISPLAY_DATE_FORMAT)
} catch (exception: DateTimeParseException) {
    this
}
