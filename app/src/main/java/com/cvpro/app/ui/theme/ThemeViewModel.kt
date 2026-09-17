package com.cvpro.app.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cvpro.app.domain.model.ThemePreference
import com.cvpro.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Tema guardado, para que la raíz de la app sepa con qué colores pintar.
 *
 * Vive en la Activity y no dentro de una pantalla, porque el tema envuelve todo.
 */
@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val themePreference: StateFlow<ThemePreference> = settingsRepository.themePreference
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            // Hasta que se lea el archivo, se usa el ajuste del teléfono.
            initialValue = ThemePreference.SYSTEM,
        )

    /**
     * Cambia entre claro y oscuro con el botón de la barra superior.
     *
     * Recibe lo que se está viendo ahora, porque si la preferencia es "seguir al sistema"
     * hay que saber cómo se ve para pasar a lo contrario. A partir del primer toque queda
     * una elección explícita, que es la que se guarda.
     */
    fun toggleTheme(isCurrentlyDark: Boolean) {
        val nueva = if (isCurrentlyDark) ThemePreference.LIGHT else ThemePreference.DARK
        viewModelScope.launch { settingsRepository.setThemePreference(nueva) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
