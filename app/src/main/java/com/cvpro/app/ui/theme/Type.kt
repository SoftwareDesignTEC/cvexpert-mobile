package com.cvpro.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.cvpro.app.R

/**
 * Tipografía de CV Pro, según la hoja de diseño de Samuel:
 * - Plus Jakarta Sans para títulos (estilos display, headline y title).
 * - Inter para el texto (estilos body y label).
 *
 * Los archivos van dentro de la app (res/font) en vez de descargarse al abrirla: así se ven
 * igual sin internet (modo offline) y no hace falta ninguna dependencia extra.
 * Licencia SIL Open Font License: ver docs/licencias.
 */

/**
 * Arma una familia a partir de una fuente variable.
 *
 * Una fuente variable trae todos los pesos en un solo archivo. Por cada peso que usa la app
 * se declara un Font que apunta al mismo archivo; en Android 8+ (minSdk 26) Compose le aplica
 * ese grosor al eje de peso de la fuente automáticamente.
 */
private fun variableFontFamily(resId: Int): FontFamily = FontFamily(
    listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold).map { weight ->
        Font(resId = resId, weight = weight)
    },
)

val PlusJakartaSans = variableFontFamily(R.font.plus_jakarta_sans)
val Inter = variableFontFamily(R.font.inter)

// Tamaños, interlineados y espaciados de la escala estándar de Material 3; solo cambia la fuente.
private val base = Typography()

val CvProTypography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = PlusJakartaSans),
    displayMedium = base.displayMedium.copy(fontFamily = PlusJakartaSans),
    displaySmall = base.displaySmall.copy(fontFamily = PlusJakartaSans),
    // Título principal de pantalla: en negrita, como estaba antes del cambio de fuente.
    headlineLarge = base.headlineLarge.copy(fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold),
    headlineMedium = base.headlineMedium.copy(fontFamily = PlusJakartaSans),
    headlineSmall = base.headlineSmall.copy(fontFamily = PlusJakartaSans),
    titleLarge = base.titleLarge.copy(fontFamily = PlusJakartaSans),
    titleMedium = base.titleMedium.copy(fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold),
    titleSmall = base.titleSmall.copy(fontFamily = PlusJakartaSans),
    bodyLarge = base.bodyLarge.copy(fontFamily = Inter),
    bodyMedium = base.bodyMedium.copy(fontFamily = Inter),
    bodySmall = base.bodySmall.copy(fontFamily = Inter),
    labelLarge = base.labelLarge.copy(fontFamily = Inter),
    labelMedium = base.labelMedium.copy(fontFamily = Inter),
    labelSmall = base.labelSmall.copy(fontFamily = Inter),
)
