package com.cvpro.app.ui.screens.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cvpro.app.BuildConfig
import com.cvpro.app.domain.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Contador de no leídas para la insignia de la campana de la barra superior.
 *
 * Se pide cuando alguien inicia sesión y cada vez que se cierra el centro de notificaciones
 * (ahí es donde el usuario marca como leídas). No se pide al crearse: antes del login no hay
 * token. Si falla, la insignia simplemente no se muestra: no es motivo para interrumpir al
 * usuario con un error.
 */
@HiltViewModel
class NotificationBadgeViewModel @Inject constructor(
    private val repository: NotificationRepository,
) : ViewModel() {

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    fun refresh() {
        if (!BuildConfig.NOTIFICATIONS_ENABLED) return
        viewModelScope.launch {
            runCatching { repository.getUnreadCount() }
                .onSuccess { _unreadCount.value = it }
        }
    }
}
