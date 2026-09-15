package com.cvpro.app.domain.repository

import android.content.Context
import com.cvpro.app.domain.model.User
import kotlinx.coroutines.flow.StateFlow

/**
 * Operaciones de sesión de la app.
 *
 * Las pantallas solo conocen esta interfaz; la implementación con Google vive en
 * data/repository. Si en una fase futura el login pasa por un backend, se cambia la
 * implementación y las pantallas quedan igual.
 */
interface AuthRepository {

    /** Usuario con sesión activa, o null si no hay. La UI lo observa para decidir qué pantalla mostrar. */
    val currentUser: StateFlow<User?>

    /**
     * Abre el selector de cuentas de Google.
     *
     * Recibe el Context de la Activity porque Android necesita una ventana visible sobre la
     * cual mostrar el selector; con el Context de la aplicación no funciona.
     */
    suspend fun signIn(activityContext: Context): SignInResult

    suspend fun signOut()
}

/** Resultado de un intento de inicio de sesión. Cada caso se traduce a un mensaje distinto en la UI. */
sealed interface SignInResult {
    data class Success(val user: User) : SignInResult

    /** El usuario cerró el selector. No es un error: no se muestra mensaje. */
    data object Cancelled : SignInResult

    /** No hay ninguna cuenta de Google en el dispositivo. */
    data object NoGoogleAccount : SignInResult

    /** Falta el Web client ID en la configuración del build. */
    data object MissingConfiguration : SignInResult

    data class Failure(val cause: Throwable) : SignInResult
}
