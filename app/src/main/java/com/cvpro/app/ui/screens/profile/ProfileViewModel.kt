package com.cvpro.app.ui.screens.profile

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cvpro.app.R
import com.cvpro.app.domain.repository.AuthRepository
import com.cvpro.app.domain.repository.ProfileRepository
import com.cvpro.app.ui.components.FormValidation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val location: String = "",

    @param:StringRes val nameError: Int? = null,
    @param:StringRes val emailError: Int? = null,
    @param:StringRes val phoneError: Int? = null,
    @param:StringRes val locationError: Int? = null,

    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
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
    authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _messages = Channel<Int>()
    val messages: Flow<Int> = _messages.receiveAsFlow()

    /*
     * En esta fase usamos el correo de Google como identificador
     * para /profiles/{id}.
     */
    private var profileId: String? = null

    init {
        val user = authRepository.currentUser.value

        if (user != null) {
            profileId = user.email

            // Se muestran inmediatamente los datos que ya conocemos por Google.
            _uiState.update {
                it.copy(
                    name = user.name,
                    email = user.email,
                )
            }

            loadProfile(user.email)
        }
    }

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
                        name = profile.name,
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
                        error = exception.message,
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
                profileRepository.updateProfile(
                    id = id,
                    name = state.name.trim(),
                    email = state.email.trim(),
                    phone = state.phone.trim().takeIf { it.isNotEmpty() },
                    location = state.location.trim(),
                )

                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = null,
                    )
                }

                _messages.send(R.string.profile_save_success)

            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = exception.message,
                    )
                }

                _messages.send(R.string.profile_save_error)
            }
        }
    }
}