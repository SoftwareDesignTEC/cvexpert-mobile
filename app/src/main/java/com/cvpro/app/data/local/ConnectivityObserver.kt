package com.cvpro.app.data.local

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dice si el teléfono tiene internet, y avisa cuando eso cambia.
 *
 * Lo usa el indicador "En línea / Sin conexión" de la barra superior, que en el diseño de
 * Samuel está en todas las pantallas. Más adelante también sirve para el modo sin conexión.
 *
 * Se apoya en ConnectivityManager, que es la forma que da Android de preguntar por la red:
 * en vez de consultar cada tanto, se registra un aviso y el sistema llama cuando hay cambios.
 */
@Singleton
class ConnectivityObserver @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    private val manager = context.getSystemService(ConnectivityManager::class.java)

    /**
     * Estado de la conexión, empezando por el que hay al momento de suscribirse.
     *
     * `callbackFlow` adapta el aviso de Android (que trabaja con llamadas) a un Flow. El
     * `awaitClose` da de baja el aviso cuando nadie lo está escuchando, para no dejar fugas.
     */
    val isOnline: Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }

            override fun onLost(network: Network) {
                trySend(currentlyOnline())
            }

            override fun onCapabilitiesChanged(
                network: Network,
                capabilities: NetworkCapabilities,
            ) {
                trySend(capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
            }
        }

        // Valor inicial: sin esto la barra se quedaría en blanco hasta el primer cambio de red.
        trySend(currentlyOnline())

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        manager?.registerNetworkCallback(request, callback)

        awaitClose { manager?.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()

    /** Estado de la red en este instante. */
    private fun currentlyOnline(): Boolean {
        val capabilities = manager?.getNetworkCapabilities(manager.activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
