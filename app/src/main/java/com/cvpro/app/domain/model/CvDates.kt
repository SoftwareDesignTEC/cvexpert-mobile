package com.cvpro.app.domain.model

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/*
 * Cómo se muestran las fechas del currículum (experiencia, estudios, certificaciones).
 *
 * Viven en el dominio porque las usan tanto las pantallas como el PDF, y no dependen de
 * Android.
 *
 * Se formatean en UTC a propósito, no en la zona del teléfono. Estas fechas son de calendario
 * ("empecé en junio de 2024"): el selector de fechas de Material 3 y el servicio las guardan
 * como la medianoche UTC de ese día. En Costa Rica (UTC-6) esa medianoche cae a las 6 p.m. del
 * día anterior, así que "1 de junio" se vería como "31 de mayo" y el mes saldría mal.
 */

private val MONTH_FORMAT = DateTimeFormatter.ofPattern("MM/yyyy")
private val DAY_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy")

/** "06/2024", que es como se acostumbra poner los periodos en un CV. Sin fecha, texto vacío. */
fun Long?.toCvMonth(): String =
    this?.let { Instant.ofEpochMilli(it).atOffset(ZoneOffset.UTC).format(MONTH_FORMAT) }.orEmpty()

/** "01/06/2024", para el campo del editor, donde se elige el día exacto. Sin fecha, vacío. */
fun Long?.toCvDay(): String =
    this?.let { Instant.ofEpochMilli(it).atOffset(ZoneOffset.UTC).format(DAY_FORMAT) }.orEmpty()
