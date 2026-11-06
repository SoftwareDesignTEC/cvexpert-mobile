package com.cvpro.app.ui.screens.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cvpro.app.BuildConfig
import com.cvpro.app.R
import com.cvpro.app.domain.model.AppNotification
import com.cvpro.app.domain.repository.NotificationRepository
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
 * Estado del centro de notificaciones.
 *
 * @property loadFailed no se pudo cargar la primera página: se muestra error y reintentar.
 * @property hasMore quedan páginas por pedir; muestra "Cargar más".
 */
data class NotificationsUiState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val notifications: List<AppNotification> = emptyList(),
    val unreadOnly: Boolean = false,
    val page: Int = 0,
    val hasMore: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isMarkingAll: Boolean = false,
) {
    val hasUnread: Boolean get() = notifications.any { !it.read }
}

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val repository: NotificationRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    // Errores de acciones sueltas (cargar más, marcar), que no tapan la lista ya cargada.
    private val _messages = Channel<Int>()
    val messages: Flow<Int> = _messages.receiveAsFlow()

    // No se carga en init: el diálogo llama load() cada vez que se abre (ver su LaunchedEffect).

    /** Vuelve a la primera página. Lo usa el reintentar y el cambio de filtro. */
    fun load() {
        if (!BuildConfig.NOTIFICATIONS_ENABLED) return
        _uiState.update { it.copy(isLoading = true, loadFailed = false) }
        viewModelScope.launch {
            try {
                val result = repository.getNotifications(page = 1, pageSize = PAGE_SIZE, unreadOnly = _uiState.value.unreadOnly)
                _uiState.update {
                    it.copy(isLoading = false, notifications = result.items, page = 1, hasMore = result.hasMore)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, loadFailed = true) }
            }
        }
    }

    fun onFilterChange(unreadOnly: Boolean) {
        if (unreadOnly == _uiState.value.unreadOnly) return
        _uiState.update { it.copy(unreadOnly = unreadOnly) }
        load()
    }

    fun onLoadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMore) return
        _uiState.update { it.copy(isLoadingMore = true) }
        viewModelScope.launch {
            try {
                val next = state.page + 1
                val result = repository.getNotifications(page = next, pageSize = PAGE_SIZE, unreadOnly = state.unreadOnly)
                _uiState.update {
                    it.copy(
                        isLoadingMore = false,
                        // Si llegó una nueva entre páginas, la siguiente repite la última: se evita el duplicado.
                        notifications = (it.notifications + result.items).distinctBy { n -> n.id },
                        page = next,
                        hasMore = result.hasMore,
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingMore = false) }
                _messages.send(R.string.notifications_error_action)
            }
        }
    }

    /**
     * Tocar una notificación la marca como leída. Se pinta leída de inmediato y, si el
     * servicio falla, vuelve a como estaba y se avisa.
     */
    fun onNotificationClick(notification: AppNotification) {
        if (notification.read) return
        setRead(notification.id, read = true)
        viewModelScope.launch {
            try {
                repository.markRead(notification.id)
            } catch (e: Exception) {
                setRead(notification.id, read = false)
                _messages.send(R.string.notifications_error_action)
            }
        }
    }

    fun onMarkAllRead() {
        if (_uiState.value.isMarkingAll) return
        _uiState.update { it.copy(isMarkingAll = true) }
        viewModelScope.launch {
            try {
                repository.markAllRead()
                _uiState.update { state ->
                    state.copy(
                        isMarkingAll = false,
                        // Con el filtro "No leídas", después de marcar todas no queda ninguna.
                        notifications = if (state.unreadOnly) emptyList() else state.notifications.map { it.copy(read = true) },
                        hasMore = if (state.unreadOnly) false else state.hasMore,
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isMarkingAll = false) }
                _messages.send(R.string.notifications_error_action)
            }
        }
    }

    private fun setRead(id: String, read: Boolean) = _uiState.update { state ->
        state.copy(notifications = state.notifications.map { if (it.id == id) it.copy(read = read) else it })
    }

    private companion object {
        const val PAGE_SIZE = 20
    }
}
