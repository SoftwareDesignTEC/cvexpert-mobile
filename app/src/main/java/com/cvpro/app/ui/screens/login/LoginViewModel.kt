package com.cvpro.app.ui.screens.login

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cvpro.app.R
import com.cvpro.app.domain.repository.AuthRepository
import com.cvpro.app.domain.repository.SignInResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Estado de la pantalla de login: un solo objeto en vez de variables sueltas.
 *
 * El caso de éxito no vive acá: cuando el login funciona, la sesión cambia en
 * AuthRepository y la app pasa sola a la pantalla principal.
 *
 * @property errorMessage id de un string en español, o null si no hay error.
 */
data class LoginUiState(
    val isLoading: Boolean = false,
    @param:StringRes val errorMessage: Int? = null,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /**
     * Inicia el login. Si ya hay uno en curso no hace nada: evita abrir dos selectores
     * cuando el usuario toca el botón dos veces seguidas.
     */
    fun signIn(activityContext: Context) {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val result = authRepository.signIn(activityContext)
            _uiState.value = when (result) {
                // Éxito o cancelación: se vuelve al estado inicial, sin mensaje.
                is SignInResult.Success, SignInResult.Cancelled -> LoginUiState()
                SignInResult.NoGoogleAccount -> LoginUiState(errorMessage = R.string.login_error_no_account)
                SignInResult.MissingConfiguration -> LoginUiState(errorMessage = R.string.login_error_configuration)
                is SignInResult.Failure -> LoginUiState(errorMessage = R.string.login_error_generic)
            }
        }
    }
}
