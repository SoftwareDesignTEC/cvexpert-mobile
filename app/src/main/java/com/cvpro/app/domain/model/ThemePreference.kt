package com.cvpro.app.domain.model

/**
 * Cómo quiere el usuario ver la app.
 *
 * SYSTEM sigue el ajuste del teléfono; LIGHT y DARK lo fuerzan. Es lo que se guarda en el
 * dispositivo, y el enunciado lo permite: es una preferencia, no datos del usuario.
 */
enum class ThemePreference {
    SYSTEM,
    LIGHT,
    DARK,
    ;

    /** Decide si toca tema oscuro, según la preferencia y el ajuste del sistema. */
    fun isDark(systemInDarkTheme: Boolean): Boolean = when (this) {
        SYSTEM -> systemInDarkTheme
        LIGHT -> false
        DARK -> true
    }
}
