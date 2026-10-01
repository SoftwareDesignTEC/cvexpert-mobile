package com.cvpro.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cvpro.app.domain.model.ThemePreference
import com.cvpro.app.domain.repository.NotificationSettings
import com.cvpro.app.domain.repository.NotificationType
import com.cvpro.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Estado de Configuración: el tema y los avisos. */
data class SettingsUiState(
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
    val notifications: NotificationSettings = NotificationSettings(),
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    // Se combinan los dos flujos: cualquiera de los dos que cambie repinta la pantalla.
    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.themePreference,
        settingsRepository.notifications,
    ) { theme, notifications ->
        SettingsUiState(themePreference = theme, notifications = notifications)
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = SettingsUiState(),
        )

    /** Enciende o apaga un aviso. Se guarda en el dispositivo y sobrevive al cierre. */
    fun onNotificationChange(type: NotificationType, enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setNotification(type, enabled) }
    }

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
