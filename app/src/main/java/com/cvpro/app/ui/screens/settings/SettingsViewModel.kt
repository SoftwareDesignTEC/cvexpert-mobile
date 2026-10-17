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

import com.cvpro.app.domain.model.ActiveSession
import com.cvpro.app.domain.model.HelpContact
import com.cvpro.app.domain.model.LegalDocument
import com.cvpro.app.domain.repository.AuthRepository
import com.cvpro.app.domain.repository.SettingsContentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** Estado de Configuración: el tema y los avisos. */
data class SettingsUiState(
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
    val notifications: NotificationSettings = NotificationSettings(),

    val sessions: List<ActiveSession> = emptyList(),
    val isLoadingSessions: Boolean = false,

    val privacyPolicy: LegalDocument? = null,
    val terms: LegalDocument? = null,
    val isLoadingLegal: Boolean = false,

    val exportContent: String? = null,
    val isExporting: Boolean = false,

    val helpContacts: List<HelpContact> = emptyList(),

    val error: String? = null,
)

private data class RemoteSettingsState(
    val sessions: List<ActiveSession> = emptyList(),
    val isLoadingSessions: Boolean = false,

    val privacyPolicy: LegalDocument? = null,
    val terms: LegalDocument? = null,
    val isLoadingLegal: Boolean = false,

    val exportContent: String? = null,
    val isExporting: Boolean = false,

    val helpContacts: List<HelpContact> = emptyList(),

    val error: String? = null,
)


@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val settingsContentRepository: SettingsContentRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val remoteState = MutableStateFlow(
        RemoteSettingsState()
    )

    // Se combinan los dos flujos: cualquiera de los dos que cambie repinta la pantalla.
    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.themePreference,
        settingsRepository.notifications,
        remoteState,
    ) { theme, notifications, remote ->

        SettingsUiState(
            themePreference = theme,
            notifications = notifications,

            sessions = remote.sessions,
            isLoadingSessions = remote.isLoadingSessions,

            privacyPolicy = remote.privacyPolicy,
            terms = remote.terms,
            isLoadingLegal = remote.isLoadingLegal,

            exportContent = remote.exportContent,
            isExporting = remote.isExporting,

            helpContacts = remote.helpContacts,

            error = remote.error,
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = SettingsUiState(),
        )

    init {
        loadHelpContacts()
    }

    /**
     * Pide los canales de contacto del centro de ayuda.
     *
     * Si falla no se muestra un error: la tarjeta de ayuda cae de vuelta al botón genérico
     * que abre el correo sin destinatario, que es lo que hacía antes.
     */
    private fun loadHelpContacts() {
        viewModelScope.launch {
            runCatching { settingsContentRepository.getHelpContacts() }
                .onSuccess { contacts ->
                    remoteState.update { it.copy(helpContacts = contacts) }
                }
        }
    }

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

    fun loadSessions() {
        val userId = authRepository.currentUser.value?.id ?: return

        viewModelScope.launch {

            remoteState.update {
                it.copy(
                    isLoadingSessions = true,
                    error = null,
                )
            }

            try {
                val sessions =
                    settingsContentRepository.getSessions(userId)

                remoteState.update {
                    it.copy(
                        sessions = sessions,
                        isLoadingSessions = false,
                    )
                }
            } catch (exception: Exception) {
                remoteState.update {
                    it.copy(
                        isLoadingSessions = false,
                        error = exception.message,
                    )
                }
            }
        }
    }

    fun loadPrivacyPolicy() {
        viewModelScope.launch {

            remoteState.update {
                it.copy(
                    isLoadingLegal = true,
                    error = null,
                )
            }

            try {
                val document =
                    settingsContentRepository.getPrivacyPolicy()

                remoteState.update {
                    it.copy(
                        privacyPolicy = document,
                        isLoadingLegal = false,
                    )
                }
            } catch (exception: Exception) {
                remoteState.update {
                    it.copy(
                        isLoadingLegal = false,
                        error = exception.message,
                    )
                }
            }
        }
    }

    fun loadTerms() {
        viewModelScope.launch {

            remoteState.update {
                it.copy(
                    isLoadingLegal = true,
                    error = null,
                )
            }

            try {
                val document =
                    settingsContentRepository.getTerms()

                remoteState.update {
                    it.copy(
                        terms = document,
                        isLoadingLegal = false,
                    )
                }
            } catch (exception: Exception) {
                remoteState.update {
                    it.copy(
                        isLoadingLegal = false,
                        error = exception.message,
                    )
                }
            }
        }
    }

    fun requestDataExport() {
        val userId = authRepository.currentUser.value?.id ?: return

        viewModelScope.launch {

            remoteState.update {
                it.copy(
                    isExporting = true,
                    error = null,
                )
            }

            try {
                val content =
                    settingsContentRepository.exportUserData(userId)

                remoteState.update {
                    it.copy(
                        exportContent = content,
                        isExporting = false,
                    )
                }
            } catch (exception: Exception) {
                remoteState.update {
                    it.copy(
                        isExporting = false,
                        error = exception.message,
                    )
                }
            }
        }
    }

    fun onExportConsumed() {
        remoteState.update {
            it.copy(exportContent = null)
        }
    }
}
