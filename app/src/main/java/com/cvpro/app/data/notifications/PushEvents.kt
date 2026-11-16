package com.cvpro.app.data.notifications

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Puente entre lo que pasa fuera de la UI (llega un push, se toca un aviso) y las pantallas.
 *
 * Es un `object` y no algo inyectado con Hilt porque lo usan el servicio de FCM y MainActivity,
 * que Android crea por su cuenta, y lo único que guarda son dos señales sin datos del usuario.
 */
object PushEvents {

    private val _received = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Llegó un push con la app abierta: la campana vuelve a pedir el número de no leídas. */
    val received: SharedFlow<Unit> = _received.asSharedFlow()

    private val _openRequested = MutableStateFlow(false)

    /**
     * El usuario tocó un aviso: hay que abrir el centro de notificaciones. Es un estado y no un
     * evento porque, si la app venía cerrada, la UI todavía no existe cuando llega el pedido.
     */
    val openRequested: StateFlow<Boolean> = _openRequested.asStateFlow()

    fun onPushReceived() {
        _received.tryEmit(Unit)
    }

    fun requestOpen() {
        _openRequested.value = true
    }

    fun onOpenHandled() {
        _openRequested.value = false
    }
}
