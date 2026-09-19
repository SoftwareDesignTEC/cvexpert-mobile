package com.cvpro.app.ui.screens.profile

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cvpro.app.R
import com.cvpro.app.domain.repository.AuthRepository
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

/**
 * Estado del formulario de Mi Perfil.
 *
 * Los nombres de los campos son los de UserProfile en el contrato de la web, para no perder
 * la paridad. Cada campo tiene su propio error, que es null mientras el valor esté bien.
 */
data class ProfileUiState(
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val location: String = "",
    @param:StringRes val nameError: Int? = null,
    @param:StringRes val emailError: Int? = null,
    @param:StringRes val phoneError: Int? = null,
    @param:StringRes val locationError: Int? = null,
) {
    /** El botón de guardar se habilita solo si hay algo escrito en los campos obligatorios. */
    val canSave: Boolean = name.isNotBlank() && email.isNotBlank() && location.isNotBlank()
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    // Avisos de una sola vez (el snackbar). Un Channel evita que el mensaje se repita al
    // girar la pantalla, cosa que sí pasaría si fuera parte del estado.
    private val _messages = Channel<Int>()
    val messages: Flow<Int> = _messages.receiveAsFlow()

    init {
        // El nombre y el correo ya los sabemos por la sesión de Google: se precargan.
        val user = authRepository.currentUser.value
        if (user != null) {
            _uiState.update { it.copy(name = user.name, email = user.email) }
        }
    }

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value, nameError = null) }

    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value, emailError = null) }

    fun onPhoneChange(value: String) = _uiState.update { it.copy(phone = value, phoneError = null) }

    fun onLocationChange(value: String) = _uiState.update { it.copy(location = value, locationError = null) }

    /**
     * Valida los cuatro campos. Si alguno falla, marca los errores y no guarda nada.
     *
     * TODO: cuando existan los endpoints del APIM, acá va el envío al servicio.
     */
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

        val hayErrores = listOfNotNull(nameError, emailError, phoneError, locationError).isNotEmpty()
        viewModelScope.launch {
            _messages.send(if (hayErrores) R.string.profile_form_invalid else R.string.profile_saved_pending)
        }
    }
}
