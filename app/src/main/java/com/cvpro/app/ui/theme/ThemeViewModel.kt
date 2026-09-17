package com.cvpro.app.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cvpro.app.domain.model.ThemePreference
import com.cvpro.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Tema guardado, para que la raíz de la app sepa con qué colores pintar.
 *
 * Vive en la Activity y no dentro de una pantalla, porque el tema envuelve todo.
 */
@HiltViewModel
class ThemeViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val themePreference: StateFlow<ThemePreference> = settingsRepository.themePreference
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            // Hasta que se lea el archivo, se usa el ajuste del teléfono.
            initialValue = ThemePreference.SYSTEM,
        )

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
