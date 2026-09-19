package com.cvpro.app.domain.repository

import com.cvpro.app.domain.model.ThemePreference
import kotlinx.coroutines.flow.Flow

/** Preferencias de la app que sobreviven al cierre. */
interface SettingsRepository {

    /** Tema elegido. Emite de nuevo cada vez que cambia, así la app se repinta sola. */
    val themePreference: Flow<ThemePreference>

    suspend fun setThemePreference(preference: ThemePreference)
}
