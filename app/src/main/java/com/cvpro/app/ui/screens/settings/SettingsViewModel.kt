package com.cvpro.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cvpro.app.domain.model.ThemePreference
import com.cvpro.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Estado de Configuración. Por ahora, solo el tema. */
data class SettingsUiState(
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = settingsRepository.themePreference
        .map { SettingsUiState(themePreference = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = SettingsUiState(),
        )

    /**
     * Guarda el tema elegido. No hace falta avisarle a la pantalla: al guardarse, el flujo
     * emite el valor nuevo y toda la app se repinta sola.
     */
    fun onThemeSelected(preference: ThemePreference) {
        viewModelScope.launch { settingsRepository.setThemePreference(preference) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
