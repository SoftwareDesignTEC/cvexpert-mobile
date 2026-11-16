package com.cvpro.app.data.notifications

import android.content.Context
import android.util.Log
import com.cvpro.app.BuildConfig
import com.cvpro.app.data.remote.api.NotificationApi
import com.cvpro.app.data.remote.dto.DeviceRegistrationDto
import com.cvpro.app.domain.repository.AuthRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Mantiene registrado en Notificaciones el token de FCM de este teléfono, para que le lleguen
 * los push.
 *
 * Es lo que pide el contrato de Samuel (sección 6 del README de cvexpert-ms-notificaciones):
 * - Al iniciar sesión: `POST /notifications/devices` con el token. Lo cubre [start], que
 *   escucha la sesión igual que NotificationDirectorySync.
 * - Cuando FCM entrega un token nuevo: lo mismo. Lo llama CvMessagingService con [onNewToken].
 * - Al cerrar sesión: `DELETE /notifications/devices/{token}`, con [unregister]. Tiene que ir
 *   ANTES de borrar la sesión, porque la llamada necesita el token de Google del usuario.
 *
 * Igual que el directorio, va detrás de NOTIFICATIONS_ENABLED, y si falla no se ve nada: es un
 * registro de fondo que no debe impedir usar la app. Se reintenta en el próximo inicio de sesión.
 *
 * Si la app se compiló sin google-services.json, Firebase no existe y esta clase no hace nada.
 */
@Singleton
class PushTokenSync @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository,
    private val api: NotificationApi,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()

    /** Último registro que el servicio aceptó: usuario y token. */
    private var lastRegistered: Pair<String, String>? = null

    private val enabled: Boolean
        get() = BuildConfig.NOTIFICATIONS_ENABLED && FirebaseApp.getApps(context).isNotEmpty()

    /** Lo llama CvApplication una sola vez, al arrancar. */
    fun start() {
        if (!enabled) return
        scope.launch {
            authRepository.currentUser.collect { user ->
                if (user == null) {
                    mutex.withLock { lastRegistered = null }
                } else {
                    val token = currentToken() ?: return@collect
                    register(user.id, token)
                }
            }
        }
    }

    /** FCM cambió el token (reinstalación, datos borrados…): se registra el nuevo. */
    fun onNewToken(token: String) {
        if (!enabled) return
        val userId = authRepository.currentUser.value?.id ?: return
        scope.launch { register(userId, token) }
    }

    /**
     * Da de baja este teléfono antes de cerrar sesión, para que los avisos de esta cuenta no le
     * sigan llegando a quien lo use después. Espera como mucho [UNREGISTER_TIMEOUT_MS]: sin
     * internet, cerrar sesión no se puede quedar pegado. En ese caso el token queda registrado
     * hasta que otra cuenta inicie sesión en el teléfono, que se lo queda (el contrato lo pasa
     * al usuario nuevo).
     */
    suspend fun unregister() {
        if (!enabled) return
        val token = mutex.withLock { lastRegistered?.second } ?: return
        withTimeoutOrNull(UNREGISTER_TIMEOUT_MS) {
            try {
                val response = api.unregisterDevice(URLEncoder.encode(token, Charsets.UTF_8.name()))
                if (!response.isSuccessful) Log.w(TAG, "Unregister device answered ${response.code()}")
            } catch (e: IOException) {
                Log.w(TAG, "Could not unregister the device", e)
            }
        }
        mutex.withLock { lastRegistered = null }
    }

    private suspend fun register(userId: String, token: String) = mutex.withLock {
        if (lastRegistered == userId to token) return@withLock
        try {
            val response = api.registerDevice(DeviceRegistrationDto(token))
            if (response.isSuccessful) {
                lastRegistered = userId to token
            } else {
                Log.w(TAG, "Register device answered ${response.code()}")
            }
        } catch (e: IOException) {
            Log.w(TAG, "Could not register the device", e)
        }
    }

    /**
     * El token de FCM de este teléfono, o null si no se pudo obtener (por ejemplo, sin Google
     * Play). Se arma a mano con un callback para no sumar otra librería solo por `await()`.
     */
    private suspend fun currentToken(): String? = suspendCancellableCoroutine { continuation ->
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) Log.w(TAG, "Could not get the FCM token", task.exception)
            continuation.resume(if (task.isSuccessful) task.result else null)
        }
    }

    private companion object {
        const val TAG = "PushTokenSync"
        const val UNREGISTER_TIMEOUT_MS = 5_000L
    }
}
