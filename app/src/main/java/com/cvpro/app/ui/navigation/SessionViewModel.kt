package com.cvpro.app.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cvpro.app.data.local.ConnectivityObserver
import com.cvpro.app.domain.model.User
import com.cvpro.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Expone la sesión y el estado de la conexión a la raíz de la app, y permite cerrar sesión. */
@HiltViewModel
class SessionViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    connectivityObserver: ConnectivityObserver,
) : ViewModel() {

    val currentUser: StateFlow<User?> = authRepository.currentUser

    /**
     * Si hay internet, para el indicador de la barra superior.
     *
     * `stateIn` convierte el Flow en un estado que la barra puede leer en cualquier momento;
     * `WhileSubscribed` da de baja el aviso del sistema cuando la app queda en segundo plano.
     */
    val isOnline: StateFlow<Boolean> = connectivityObserver.isOnline
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = true,
        )

    fun signOut() {
        viewModelScope.launch { authRepository.signOut() }
    }

    private companion object {
        /** Margen para que un giro de pantalla no dé de baja y vuelva a dar de alta el aviso. */
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
