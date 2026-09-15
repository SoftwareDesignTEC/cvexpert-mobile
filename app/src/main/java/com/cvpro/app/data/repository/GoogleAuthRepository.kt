package com.cvpro.app.data.repository

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.cvpro.app.BuildConfig
import com.cvpro.app.domain.model.User
import com.cvpro.app.domain.repository.AuthRepository
import com.cvpro.app.domain.repository.SignInResult
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Login nativo con Google usando Credential Manager.
 *
 * Flujo: se le pide a Credential Manager una credencial de tipo "Google ID token" para el
 * Web client ID del proyecto. Android muestra el selector de cuentas y, si el usuario elige
 * una, devuelve un token con su nombre, correo y foto.
 *
 * Fase I no tiene backend, así que el token no se valida contra un servidor: sus datos se
 * usan para la sesión local. La sesión vive solo en memoria porque el enunciado no permite
 * datos permanentes en el dispositivo; al reabrir la app, el selector recuerda la cuenta.
 *
 * @Singleton: una sola instancia para toda la app, así todas las pantallas ven la misma sesión.
 */
@Singleton
class GoogleAuthRepository @Inject constructor(
    @ApplicationContext appContext: Context,
) : AuthRepository {

    private val credentialManager = CredentialManager.create(appContext)

    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    override suspend fun signIn(activityContext: Context): SignInResult {
        // Viene de local.properties (local) o de un secret de GitHub Actions (CI), vía BuildConfig.
        val webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
        if (webClientId.isBlank()) return SignInResult.MissingConfiguration

        val googleIdOption = GetGoogleIdOption.Builder()
            // false = muestra todas las cuentas del teléfono, no solo las que ya entraron a la app.
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            // Si hay una sola cuenta que ya autorizó la app, entra sin mostrar el selector.
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val credential = credentialManager.getCredential(activityContext, request).credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val user = User(
                    // En este tipo de credencial, id es el correo de la cuenta.
                    name = googleCredential.displayName ?: googleCredential.id,
                    email = googleCredential.id,
                    photoUrl = googleCredential.profilePictureUri?.toString(),
                )
                _currentUser.value = user
                SignInResult.Success(user)
            } else {
                SignInResult.Failure(IllegalStateException("Unexpected credential type: ${credential.type}"))
            }
        } catch (e: GetCredentialCancellationException) {
            SignInResult.Cancelled
        } catch (e: NoCredentialException) {
            SignInResult.NoGoogleAccount
        } catch (e: GetCredentialException) {
            Log.w(TAG, "Google sign-in failed", e)
            SignInResult.Failure(e)
        } catch (e: GoogleIdTokenParsingException) {
            Log.w(TAG, "Invalid Google ID token", e)
            SignInResult.Failure(e)
        }
    }

    override suspend fun signOut() {
        try {
            // Avisa a los proveedores de credenciales que se cerró la sesión, para que la
            // próxima vez no entre automáticamente con la misma cuenta.
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: ClearCredentialException) {
            Log.w(TAG, "Could not clear credential state", e)
        }
        _currentUser.value = null
    }

    private companion object {
        const val TAG = "GoogleAuthRepository"
    }
}
