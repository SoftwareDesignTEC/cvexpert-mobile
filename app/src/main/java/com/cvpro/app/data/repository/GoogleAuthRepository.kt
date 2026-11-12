package com.cvpro.app.data.repository

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CredentialOption
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.cvpro.app.BuildConfig
import com.cvpro.app.data.auth.IdTokenClaims
import com.cvpro.app.domain.model.User
import com.cvpro.app.domain.repository.AuthRepository
import com.cvpro.app.domain.repository.SignInResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Login nativo con Google usando Credential Manager.
 *
 * Flujo: se le pide a Credential Manager una credencial de tipo "Google ID token" para el
 * Web client ID del proyecto. Android muestra el selector de cuentas y, si el usuario elige
 * una, devuelve un token con su nombre, correo y foto.
 *
 * Plan C: en algunos teléfonos (Honor con MagicOS) Credential Manager nunca responde. Si pasa,
 * se entra por la pantalla clásica de Google ([GoogleSignInClient]), que es una Activity de
 * Servicios de Google Play y no depende de Credential Manager. Google la marcó como obsoleta,
 * pero sigue funcionando y devuelve el mismo token.
 *
 * Fase I no tiene backend, así que el token no se valida contra un servidor: sus datos se
 * usan para la sesión local. La sesión vive solo en memoria porque el enunciado no permite
 * datos permanentes en el dispositivo; al reabrir la app, el selector recuerda la cuenta.
 *
 * @Singleton: una sola instancia para toda la app, así todas las pantallas ven la misma sesión.
 */
@Singleton
class GoogleAuthRepository @Inject constructor(
    @ApplicationContext private val appContext: Context,
) : AuthRepository {

    private val credentialManager = CredentialManager.create(appContext)

    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    // El token que devuelve Google. Antes se descartaba, porque la Fase I no tenía servicio
    // que lo validara; desde que el APIM exige autenticación, la capa de red lo necesita.
    private val _idToken = MutableStateFlow<String?>(null)
    override val idToken: StateFlow<String?> = _idToken.asStateFlow()

    private val _sessionExpired = MutableStateFlow(false)
    override val sessionExpired: StateFlow<Boolean> = _sessionExpired.asStateFlow()

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

        // Plan B: el botón oficial "Sign in with Google", que abre el selector completo de cuentas.
        // Se usa si el primer intento no encuentra cuentas o se queda colgado: en algunos teléfonos
        // la hoja de abajo de GetGoogleIdOption nunca aparece.
        val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(webClientId).build()

        return try {
            // FirstTry(null) = Google respondió que no hay cuentas para la hoja de abajo: plan B.
            // null a secas = se acabó el tiempo sin que Google abriera nada: Credential Manager
            // está colgado en este teléfono y el plan B tampoco va a responder. Directo al plan C.
            val first = withTimeoutOrNull(FIRST_TRY_TIMEOUT_MS) {
                try {
                    FirstTry(credentialManager.getCredential(activityContext, request(googleIdOption)).credential)
                } catch (e: NoCredentialException) {
                    FirstTry(null)
                }
            } ?: return SignInResult.UseClassicSignIn
            val credential = first.credential
                ?: withTimeoutOrNull(FALLBACK_TIMEOUT_MS) {
                    credentialManager.getCredential(activityContext, request(signInWithGoogleOption)).credential
                }
                ?: return SignInResult.UseClassicSignIn
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val user = User(
                    id = IdTokenClaims.subject(googleCredential.idToken) ?: googleCredential.id,
                    // En este tipo de credencial, id es el correo de la cuenta.
                    name = googleCredential.displayName ?: googleCredential.id,
                    email = googleCredential.id,
                    photoUrl = googleCredential.profilePictureUri?.toString(),
                )
                _idToken.value = googleCredential.idToken
                _currentUser.value = user
                // Entrar de nuevo borra el aviso: la sesión vencida ya quedó atrás.
                _sessionExpired.value = false
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

    private fun request(option: CredentialOption): GetCredentialRequest =
        GetCredentialRequest.Builder().addCredentialOption(option).build()

    /** Lo que devolvió el primer intento; distingue "no hay cuentas" de "no respondió". */
    private class FirstTry(val credential: Credential?)

    @Suppress("DEPRECATION") // API clásica de Google, a propósito: ver la documentación de la clase.
    private fun classicClient(context: Context): GoogleSignInClient {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .requestEmail()
            .requestProfile()
            .build()
        return GoogleSignIn.getClient(context, options)
    }

    @Suppress("DEPRECATION")
    override fun classicSignInIntent(activityContext: Context): Intent =
        classicClient(activityContext).signInIntent

    @Suppress("DEPRECATION")
    override fun completeClassicSignIn(data: Intent?): SignInResult = try {
        val account = GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException::class.java)
        val idToken = account.idToken
        val email = account.email
        if (idToken == null || email == null) {
            SignInResult.Failure(IllegalStateException("Classic sign-in without token or email"))
        } else {
            val user = User(
                id = IdTokenClaims.subject(idToken) ?: account.id ?: email,
                name = account.displayName ?: email,
                email = email,
                photoUrl = account.photoUrl?.toString(),
            )
            _idToken.value = idToken
            _currentUser.value = user
            _sessionExpired.value = false
            SignInResult.Success(user)
        }
    } catch (e: ApiException) {
        Log.w(TAG, "Classic Google sign-in failed: ${e.statusCode}", e)
        when (e.statusCode) {
            GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> SignInResult.Cancelled
            // Servicios de Google Play caídos o desactualizados.
            GoogleSignInStatusCodes.SIGN_IN_FAILED, GoogleSignInStatusCodes.INTERNAL_ERROR ->
                SignInResult.GoogleNotResponding
            else -> SignInResult.Failure(e)
        }
    }

    /**
     * El token venció o el APIM lo rechazó.
     *
     * Solo se borra la sesión en memoria. A diferencia de [signOut], no se toca el estado
     * de la credencial: el usuario no pidió salir, y limpiarlo lo obligaría a elegir la
     * cuenta otra vez para algo que no hizo.
     */
    override fun onUnauthorized() {
        // Si ya no había sesión, no hay nada que avisar: evita que varias llamadas que
        // fallan a la vez enciendan el aviso después de que el usuario ya volvió a entrar.
        if (_currentUser.value == null && _idToken.value == null) return

        _currentUser.value = null
        _idToken.value = null
        _sessionExpired.value = true
    }

    override fun onExpiryNoticeShown() {
        _sessionExpired.value = false
    }

    override suspend fun signOut() {
        try {
            // Avisa a los proveedores de credenciales que se cerró la sesión, para que la
            // próxima vez no entre automáticamente con la misma cuenta.
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: ClearCredentialException) {
            Log.w(TAG, "Could not clear credential state", e)
        }
        // Si entró por el plan C, también se cierra ahí; si no, la próxima vez entraría solo.
        @Suppress("DEPRECATION")
        runCatching { classicClient(appContext).signOut() }
        _currentUser.value = null
        _idToken.value = null
        // Salida voluntaria: no corresponde el aviso de sesión vencida.
        _sessionExpired.value = false
    }

    private companion object {
        /**
         * Lo que se espera a que Google muestre las cuentas. Si en ese tiempo no aparece nada,
         * no es que el usuario tarde en elegir: es que la hoja nunca se abrió.
         */
        const val FIRST_TRY_TIMEOUT_MS = 15_000L

        /** El selector completo da más tiempo: ahí el usuario sí puede estar eligiendo o escribiendo. */
        const val FALLBACK_TIMEOUT_MS = 90_000L

        const val TAG = "GoogleAuthRepository"
    }
}
