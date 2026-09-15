package com.cvpro.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Esquema del tema claro.
 *
 * El azul de marca es el color principal. El celeste queda como secundario y nunca se usa
 * como texto sobre blanco: ahí su contraste es 2.14, muy por debajo del mínimo de 4.5.
 */
private val LightColors = lightColorScheme(
    primary = Blue600,
    onPrimary = Color.White,
    primaryContainer = Blue100,
    onPrimaryContainer = Blue900,
    inversePrimary = Sky400,
    secondary = Sky400,
    onSecondary = Slate900,
    secondaryContainer = Sky100,
    onSecondaryContainer = Sky900,
    tertiary = Slate500,
    onTertiary = Color.White,
    tertiaryContainer = Slate100,
    onTertiaryContainer = Slate900,
    background = Color.White,
    onBackground = Slate900,
    surface = Color.White,
    onSurface = Slate900,
    surfaceVariant = Slate50,
    onSurfaceVariant = Slate500,
    surfaceTint = Blue600,
    inverseSurface = Slate900,
    inverseOnSurface = Slate50,
    error = Red600,
    onError = Color.White,
    errorContainer = Red100,
    onErrorContainer = Red900,
    outline = Slate500,
    outlineVariant = Slate200,
    scrim = Color.Black,
    // Fondos de tarjetas y campos: el gris de marca (Slate500) solo pasa el contraste
    // sobre blanco o Slate50; con Slate100 o más oscuro ya no se lee bien.
    surfaceBright = Color.White,
    surfaceDim = Slate50,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Slate50,
    surfaceContainer = Slate50,
    surfaceContainerHigh = Slate50,
    surfaceContainerHighest = Slate50,
)

/**
 * Esquema del tema oscuro.
 *
 * Acá el celeste pasa a ser el color principal: el azul de marca sobre el fondo oscuro
 * tiene contraste 3.45 y no se lee. Por la misma razón, el gris de texto sube de
 * Slate500 (3.75) a Slate400 (6.96).
 */
private val DarkColors = darkColorScheme(
    primary = Sky400,
    onPrimary = Slate900,
    primaryContainer = Sky800,
    onPrimaryContainer = Sky100,
    inversePrimary = Blue600,
    secondary = Blue600,
    onSecondary = Color.White,
    secondaryContainer = Blue900,
    onSecondaryContainer = Blue100,
    tertiary = Slate400,
    onTertiary = Slate900,
    tertiaryContainer = Slate700,
    onTertiaryContainer = Slate100,
    background = Slate900,
    onBackground = Color.White,
    surface = Slate900,
    onSurface = Color.White,
    surfaceVariant = Slate800,
    onSurfaceVariant = Slate400,
    surfaceTint = Sky400,
    inverseSurface = Slate100,
    inverseOnSurface = Slate900,
    error = Red400,
    onError = Slate900,
    errorContainer = Red900,
    onErrorContainer = Red100,
    outline = Slate400,
    outlineVariant = Slate700,
    scrim = Color.Black,
    // Los fondos elevados se aclaran hasta Slate800; Slate700 ya baja el gris de texto a 4.04.
    surfaceBright = Slate800,
    surfaceDim = Slate900,
    surfaceContainerLowest = Slate950,
    surfaceContainerLow = Slate900,
    surfaceContainer = Slate800,
    surfaceContainerHigh = Slate800,
    surfaceContainerHighest = Slate800,
)

/**
 * Tema raíz de la app. Envuelve TODA la UI.
 *
 * Decisión: NO se usa dynamic color (la paleta que Android saca del fondo de
 * pantalla del usuario) porque la entrega debe respetar el diseño de Samuel;
 * si el color cambiara por teléfono, los mockups no se podrían comparar.
 *
 * @param darkTheme por defecto sigue el ajuste del sistema (claro/oscuro).
 */
@Composable
fun CvProTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = CvProTypography,
        content = content,
    )
}
