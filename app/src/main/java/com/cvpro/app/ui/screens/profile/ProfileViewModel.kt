package com.cvpro.app.ui.screens.profile

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cvpro.app.R
import com.cvpro.app.data.location.DeviceLocationProvider
import com.cvpro.app.domain.model.UserProfile
import com.cvpro.app.domain.model.personName
import com.cvpro.app.domain.repository.AlertsRepository
import com.cvpro.app.domain.repository.AuthRepository
import com.cvpro.app.domain.repository.ProfileRepository
import com.cvpro.app.ui.components.FormValidation
import com.cvpro.app.ui.components.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val location: String = "",

    @param:StringRes val nameError: Int? = null,
    @param:StringRes val emailError: Int? = null,
    @param:StringRes val phoneError: Int? = null,
    @param:StringRes val locationError: Int? = null,

    /** Perfil completo tal como lo devuelve el servicio, para la vista de solo lectura. */
    val profile: UserProfile? = null,

    /** true mientras se está editando; false muestra la ficha del diseño. */
    val isEditing: Boolean = false,

    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    @param:StringRes val error: Int? = null,
    /** "Usar mi ubicación" está esperando al teléfono. */
    val isLocating: Boolean = false,
) {
    val canSave: Boolean =
        name.isNotBlank() &&
                email.isNotBlank() &&
                location.isNotBlank() &&
                !isLoading &&
                !isSaving
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val alertsRepository: AlertsRepository,
    private val locationProvider: DeviceLocationProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _messages = Channel<Int>()
    val messages: Flow<Int> = _messages.receiveAsFlow()

    /*
     * Id para /profiles/{id}: el sub de la cuenta de Google, como en la web y en el
     * servicio de Usuarios (ver User.id).
     */
    private var profileId: String? = null

    init {
        val user = authRepository.currentUser.value

        if (user != null) {
            profileId = user.id

            // Se muestran inmediatamente los datos que ya conocemos por Google.
            _uiState.update {
                it.copy(
                    name = personName(user.name),
                    email = user.email,
                )
            }

            loadProfile(user.id)
        }
    }

    /** Abre el formulario de edición sobre los datos ya cargados. */
    fun onEditClick() = _uiState.update { it.copy(isEditing = true) }

    /** Vuelve a la ficha sin guardar. */
    fun onCancelEdit() = _uiState.update { it.copy(isEditing = false) }

    private fun loadProfile(id: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                )
            }

            try {
                val profile = profileRepository.getProfile(id)

                _uiState.update {
                    it.copy(
                        profile = profile,
                        // Perfiles viejos guardaron el correo como nombre: se muestra vacío.
                        name = personName(profile.name).ifBlank { personName(authRepository.currentUser.value?.name) },
                        email = profile.email,
                        phone = profile.phone.orEmpty(),
                        location = profile.location,
                        isLoading = false,
                        error = null,
                    )
                }
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = exception.toUserMessage(),
                    )
                }

                _messages.send(R.string.profile_load_error)
            }
        }
    }

    fun onNameChange(value: String) =
        _uiState.update { it.copy(name = value, nameError = null) }

    fun onEmailChange(value: String) =
        _uiState.update { it.copy(email = value, emailError = null) }

    fun onPhoneChange(value: String) =
        _uiState.update { it.copy(phone = value, phoneError = null) }

    fun onLocationChange(value: String) =
        _uiState.update { it.copy(location = value, locationError = null) }

    /**
     * "Usar mi ubicación": la pantalla ya pidió el permiso. Si el teléfono da la ubicación, se
     * llena el campo ("Grecia, Alajuela"); el usuario la puede corregir antes de guardar.
     */
    fun onUseMyLocation() {
        if (_uiState.value.isLocating) return
        _uiState.update { it.copy(isLocating = true) }
        viewModelScope.launch {
            val place = locationProvider.currentPlace()
            _uiState.update {
                if (place != null) it.copy(isLocating = false, location = place, locationError = null)
                else it.copy(isLocating = false)
            }
            if (place == null) _messages.send(R.string.profile_location_unavailable)
        }
    }

    /** El usuario no dio el permiso: se le explica que puede escribirla a mano. */
    fun onLocationPermissionDenied() {
        viewModelScope.launch { _messages.send(R.string.profile_location_denied) }
    }

    fun onSaveClick() {
        val state = _uiState.value

        val nameError = FormValidation.minLength(state.name, min = 3)
        val emailError = FormValidation.email(state.email)
        val phoneError = FormValidation.phoneOptional(state.phone)
        val locationError = FormValidation.required(state.location)

        _uiState.update {
            it.copy(
                nameError = nameError,
                emailError = emailError,
                phoneError = phoneError,
                locationError = locationError,
            )
        }

        val hayErrores = listOfNotNull(
            nameError,
            emailError,
            phoneError,
            locationError,
        ).isNotEmpty()

        if (hayErrores) {
            viewModelScope.launch {
                _messages.send(R.string.profile_form_invalid)
            }
            return
        }

        val id = profileId ?: run {
            viewModelScope.launch {
                _messages.send(R.string.profile_save_error)
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSaving = true,
                    error = null,
                )
            }

            try {
                val updated = profileRepository.updateProfile(
                    id = id,
                    name = state.name.trim(),
                    email = state.email.trim(),
                    phone = state.phone.trim().takeIf { it.isNotEmpty() },
                    location = state.location.trim(),
                )

                // Al guardar bien se vuelve a la ficha, ya con los datos nuevos.
                _uiState.update {
                    it.copy(
                        profile = updated,
                        isEditing = false,
                        isSaving = false,
                        error = null,
                    )
                }

                _messages.send(R.string.profile_save_success)

                // Las alertas de ofertas usan la ubicación del perfil: se mantienen al día.
                // Si falla no se avisa, porque el perfil sí se guardó; la próxima vez que se
                // abran las alertas se toma el perfil actualizado igual.
                runCatching { alertsRepository.syncProfile(updated) }

            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = exception.toUserMessage(),
                    )
                }

                _messages.send(R.string.profile_save_error)
            }
        }
    }
}