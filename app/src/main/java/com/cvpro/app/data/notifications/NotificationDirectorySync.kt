package com.cvpro.app.data.notifications

import android.content.Context
import android.util.Log
import com.cvpro.app.BuildConfig
import com.cvpro.app.data.remote.api.NotificationApi
import com.cvpro.app.data.remote.dto.DirectoryEntryRequestDto
import com.cvpro.app.domain.repository.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mantiene al usuario registrado en el directorio de Notificaciones con el idioma del teléfono.
 *
 * Samuel pide llamar `PUT /notifications/directory/me` al iniciar sesión y cada vez que cambia
 * el idioma. Las dos cosas se cubren así:
 * - [start] escucha la sesión: cada vez que alguien entra, se registra.
 * - [onLanguageMaybeChanged] lo llama MainActivity al crearse. Android recrea la Activity cuando
 *   cambia el idioma del sistema, así que es el momento de revisarlo. Solo se reenvía si el
 *   idioma es distinto al último enviado; rotar la pantalla no genera llamadas.
 *
 * Mientras Notificaciones no esté desplegado, la bandera NOTIFICATIONS_ENABLED (BuildConfig)
 * está apagada y nada de esto hace llamadas.
 *
 * Si la llamada falla no pasa nada visible: es un registro de fondo y no debe impedir usar la
 * app. Se reintenta la próxima vez que se inicie sesión o cambie el idioma.
 */
@Singleton
class NotificationDirectorySync @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository,
    private val api: NotificationApi,
) {

    // Vive lo mismo que la app. SupervisorJob: si una llamada falla, el resto sigue.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Evita que dos registros (login y Activity creándose a la vez) se pisen.
    private val mutex = Mutex()

    /** Último registro que el servicio aceptó: usuario e idioma. */
    private var lastRegistered: Pair<String, String>? = null

    /** Lo llama CvApplication una sola vez, al arrancar. */
    fun start() {
        if (!BuildConfig.NOTIFICATIONS_ENABLED) return
        scope.launch {
            authRepository.currentUser.collect { user ->
                if (user == null) {
                    // Al salir se olvida, así el próximo login registra aunque sea la misma cuenta.
                    mutex.withLock { lastRegistered = null }
                } else {
                    register(user.id)
                }
            }
        }
    }

    /** Lo llama MainActivity al crearse; reenvía solo si el idioma cambió. */
    fun onLanguageMaybeChanged() {
        if (!BuildConfig.NOTIFICATIONS_ENABLED) return
        val userId = authRepository.currentUser.value?.id ?: return
        scope.launch { register(userId) }
    }

    private suspend fun register(userId: String) = mutex.withLock {
        val language = currentLanguage()
        if (lastRegistered == userId to language) return@withLock
        try {
            val response = api.updateDirectoryEntry(DirectoryEntryRequestDto(language))
            if (response.isSuccessful) {
                lastRegistered = userId to language
            } else {
                Log.w(TAG, "Notifications directory answered ${response.code()}")
            }
        } catch (e: IOException) {
            // Sin internet o el servicio no responde: se reintenta en el próximo login.
            Log.w(TAG, "Could not register in the notifications directory", e)
        }
    }

    /**
     * "en" si el teléfono está en inglés; "es" en cualquier otro caso, porque la app solo
     * tiene esos dos idiomas y el español es el predeterminado.
     */
    private fun currentLanguage(): String {
        val language = context.resources.configuration.locales[0].language
        return if (language == "en") "en" else "es"
    }

    private companion object {
        const val TAG = "NotificationDirectory"
    }
}
