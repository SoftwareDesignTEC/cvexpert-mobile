package com.cvpro.app.ui.components

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/*
 * Las fechas viajan en epoch milisegundos UTC (regla del contrato de la Fase II) y se
 * muestran en la zona horaria del teléfono, para que "Última actividad" diga la hora local
 * de quien la lee y no la del servidor.
 */

private val DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

/** Placeholder cuando el servicio no mandó una fecha que se pueda leer. */
private const val NO_DATE = "—"

/** "20/09/2026" en la zona del teléfono. */
fun Long?.toLocalDate(zone: ZoneId = ZoneId.systemDefault()): String =
    this?.let { Instant.ofEpochMilli(it).atZone(zone).format(DATE_FORMAT) } ?: NO_DATE

/** "20/09/2026 15:00" en la zona del teléfono. */
fun Long?.toLocalDateTime(zone: ZoneId = ZoneId.systemDefault()): String =
    this?.let { Instant.ofEpochMilli(it).atZone(zone).format(DATE_TIME_FORMAT) } ?: NO_DATE
