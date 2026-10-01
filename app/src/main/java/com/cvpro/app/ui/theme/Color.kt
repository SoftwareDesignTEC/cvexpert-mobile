package com.cvpro.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Paleta de CV Pro.
 *
 * Los cuatro colores de marca vienen de la hoja "Colores a utilizar" de Samuel (UX/UI).
 * Son colores de la paleta estándar de Tailwind, la misma que usa la web de Pérez, por
 * eso se nombran igual que en Tailwind: así web y móvil hablan de los mismos colores.
 *
 * Material 3 necesita más roles que los cuatro de la hoja (fondos de tarjetas, bordes,
 * errores...). Los que no se definen toman el violeta por defecto de Material, así que
 * se completan con tonos vecinos de las mismas familias de Tailwind (slate, blue, sky) y
 * red para errores. Cada par texto/fondo se verificó con contraste WCAG AA (4.5:1).
 *
 * Ninguna pantalla usa estas constantes directo: todo se consume desde
 * MaterialTheme.colorScheme (ver Theme.kt).
 *
 * OJO: si cambia el azul o el celeste de marca, también hay que cambiarlos en
 * res/values/colors.xml (el ícono no puede leer este archivo).
 */

// --- Colores de marca (hoja de Samuel) ---
val Blue600 = Color(0xFF2563EB)
val Slate900 = Color(0xFF0F172A)
val Sky400 = Color(0xFF38BDF8)
val Slate500 = Color(0xFF64748B)

// --- Tonos derivados (paleta de Tailwind) ---
val Slate50 = Color(0xFFF8FAFC)
val Slate100 = Color(0xFFF1F5F9)
val Slate200 = Color(0xFFE2E8F0)
val Slate400 = Color(0xFF94A3B8)
val Slate700 = Color(0xFF334155)
val Slate800 = Color(0xFF1E293B)
val Slate950 = Color(0xFF020617)

// Fondo azulado del diseño: las tarjetas blancas se recortan sobre él. Sin esto, con el
// fondo en blanco puro, las tarjetas de sección no se distinguen.
val Blue50 = Color(0xFFEEF2FF)
val Blue100 = Color(0xFFDBEAFE)
val Blue900 = Color(0xFF1E3A8A)

// Verde del porcentaje de compatibilidad de las ofertas.
val Green100 = Color(0xFFDCFCE7)
val Green700 = Color(0xFF15803D)
val Green900 = Color(0xFF14532D)
val Green300 = Color(0xFF86EFAC)

val Sky100 = Color(0xFFE0F2FE)
val Sky800 = Color(0xFF075985)
val Sky900 = Color(0xFF0C4A6E)

val Red100 = Color(0xFFFEE2E2)
val Red400 = Color(0xFFF87171)
val Red600 = Color(0xFFDC2626)
val Red900 = Color(0xFF7F1D1D)
