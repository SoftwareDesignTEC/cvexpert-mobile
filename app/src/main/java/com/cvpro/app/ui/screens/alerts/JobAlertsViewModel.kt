package com.cvpro.app.ui.screens.alerts

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cvpro.app.R
import com.cvpro.app.domain.model.JobAlertSettings
import com.cvpro.app.domain.model.JobModality
import com.cvpro.app.domain.model.UserProfile
import com.cvpro.app.domain.repository.AlertsRepository
import com.cvpro.app.domain.repository.AuthRepository
import com.cvpro.app.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

/**
 * Estado de la pantalla de alertas.
 *
 * Lo que el usuario edita va como texto o valores sueltos (el campo de años es texto para
 * poder validarlo); lo que sale del perfil va aparte, de solo lectura.
 *
 * @property loadFailed true si no se pudieron leer las alertas: se muestra error y reintentar.
 * @property experienceYears lo que escribió el usuario, sin convertir todavía.
 * @property experienceError id del mensaje de error del campo de años, o null.
 */
data class JobAlertsUiState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val isSaving: Boolean = false,
    val enabled: Boolean = false,
    val minMatch: Int = JobAlertSettings.DEFAULT_MIN_MATCH,
    val skills: List<String> = emptyList(),
    val skillInput: String = "",
    val experienceYears: String = "0",
    @param:StringRes val experienceError: Int? = null,
    val location: String = "",
    val modalities: List<JobModality> = emptyList(),
    val jobTypes: List<String> = emptyList(),
    val sectors: List<String> = emptyList(),
) {
    /**
     * El servicio exige la ubicación (sin ella responde 400) y sale del perfil. Un usuario
     * nuevo puede no tenerla todavía: en ese caso se le pide completar el perfil.
     */
    val missingLocation: Boolean get() = location.isBlank()
}

@HiltViewModel
class JobAlertsViewModel @Inject constructor(
    private val alertsRepository: AlertsRepository,
    private val profileRepository: ProfileRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(JobAlertsUiState())
    val uiState: StateFlow<JobAlertsUiState> = _uiState.asStateFlow()

    // Mensajes de una sola vez para el snackbar (guardado, error al guardar).
    private val _messages = Channel<Int>()
    val messages: Flow<Int> = _messages.receiveAsFlow()

    // No se carga en init: el diálogo llama load() cada vez que se abre (ver su LaunchedEffect).

    /**
     * Pide las alertas y el perfil a la vez.
     *
     * Las alertas mandan sobre lo que edita el usuario; el perfil manda sobre ubicación,
     * modalidades, jornadas y sectores, porque es la fuente más reciente de esos datos.
     * Si el perfil no responde, se usa lo que tenían guardado las alertas.
     */
    fun load() {
        _uiState.update { it.copy(isLoading = true, loadFailed = false) }
        viewModelScope.launch {
            val profileId = authRepository.currentUser.value?.id
            val profileRequest = async {
                profileId?.let { runCatching { profileRepository.getProfile(it) }.getOrNull() }
            }
            try {
                val alerts = alertsRepository.getAlerts()
                val profile = profileRequest.await()
                _uiState.value = stateFrom(alerts, profile)
            } catch (e: Exception) {
                profileRequest.cancel()
                _uiState.update { it.copy(isLoading = false, loadFailed = true) }
            }
        }
    }

    fun onEnabledChange(value: Boolean) = _uiState.update { it.copy(enabled = value) }

    /** El slider manda valores continuos; se redondea al múltiplo de 5 más cercano. */
    fun onMinMatchChange(value: Float) =
        _uiState.update { it.copy(minMatch = ((value / STEP).let(Math::round) * STEP).coerceIn(0, 100)) }

    fun onSkillInputChange(value: String) =
        _uiState.update { it.copy(skillInput = value.take(MAX_SKILL_LENGTH)) }

    /** Agrega lo escrito como habilidad. Ignora vacíos y repetidas sin importar mayúsculas. */
    fun onAddSkill() = _uiState.update { state ->
        val skill = state.skillInput.trim()
        val repeated = state.skills.any { it.equals(skill, ignoreCase = true) }
        if (skill.isEmpty() || repeated || state.skills.size >= MAX_SKILLS) {
            state.copy(skillInput = "")
        } else {
            state.copy(skills = state.skills + skill, skillInput = "")
        }
    }

    fun onRemoveSkill(skill: String) = _uiState.update { it.copy(skills = it.skills - skill) }

    fun onExperienceChange(value: String) =
        _uiState.update { it.copy(experienceYears = value.filter(Char::isDigit).take(2), experienceError = null) }

    fun onSaveClick() {
        val state = _uiState.value
        if (state.isSaving) return
        if (state.missingLocation) {
            viewModelScope.launch { _messages.send(R.string.alerts_missing_location) }
            return
        }

        val years = state.experienceYears.toIntOrNull()
        if (years == null || years > MAX_EXPERIENCE_YEARS) {
            _uiState.update { it.copy(experienceError = R.string.alerts_experience_error) }
            return
        }

        // Si quedó algo escrito sin tocar "Agregar", se agrega igual: es lo que el usuario espera.
        onAddSkill()
        val skills = _uiState.value.skills

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                val saved = alertsRepository.saveAlerts(
                    JobAlertSettings(
                        enabled = state.enabled,
                        minMatch = state.minMatch,
                        skills = skills,
                        experienceYears = years,
                        location = state.location,
                        modalities = state.modalities,
                        jobTypes = state.jobTypes,
                        sectors = state.sectors,
                        updatedAt = null,
                    ),
                )
                _uiState.update { it.copy(isSaving = false, enabled = saved.enabled, minMatch = saved.minMatch) }
                _messages.send(R.string.alerts_save_success)
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false) }
                // 400: el servicio rechazó algún valor. Lo demás es red o servicio caído.
                val message = if (e is HttpException && e.code() == HTTP_BAD_REQUEST) {
                    R.string.alerts_save_invalid
                } else {
                    R.string.alerts_save_error
                }
                _messages.send(message)
            }
        }
    }

    private fun stateFrom(alerts: JobAlertSettings?, profile: UserProfile?) = JobAlertsUiState(
        isLoading = false,
        enabled = alerts?.enabled ?: false,
        minMatch = alerts?.minMatch ?: JobAlertSettings.DEFAULT_MIN_MATCH,
        skills = alerts?.skills.orEmpty(),
        experienceYears = (alerts?.experienceYears ?: 0).toString(),
        location = profile?.location ?: alerts?.location.orEmpty(),
        modalities = profile?.modalities ?: alerts?.modalities.orEmpty(),
        jobTypes = profile?.types ?: alerts?.jobTypes.orEmpty(),
        sectors = profile?.sectors ?: alerts?.sectors.orEmpty(),
    )

    private companion object {
        const val STEP = 5
        const val MAX_SKILLS = 30
        const val MAX_SKILL_LENGTH = 50
        const val MAX_EXPERIENCE_YEARS = 60
        const val HTTP_BAD_REQUEST = 400
    }
}
