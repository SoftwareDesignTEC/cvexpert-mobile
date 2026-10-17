package com.cvpro.app.data.remote

import com.cvpro.app.data.auth.JwtExpiry
import com.cvpro.app.domain.repository.AuthRepository
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Le adjunta el token de Google a cada llamada al APIM.
 *
 * El APIM dejó de usar clave de suscripción y ahora valida el token de la sesión: sin este
 * encabezado, todas las peticiones responden 401 "Invalid or missing Google token".
 *
 * Un interceptor de OkHttp se mete en medio de cada petición antes de que salga, así que el
 * encabezado se pone en un solo lugar en vez de repetirlo en cada método de las interfaces.
 *
 * Además vigila la vigencia de la sesión, con el mismo criterio que la app web:
 *
 * - Antes de salir, si el token ya venció, no se manda y se cierra la sesión. Ahorra una
 *   llamada que se sabe que falla.
 * - Si el servicio responde 401, se cierra la sesión igual: puede que el APIM lo rechace
 *   por un motivo que el teléfono no puede ver.
 *
 * En los dos casos la app vuelve sola al login con un aviso, en vez de dejar siete
 * pantallas mostrando errores sueltos que no explican nada.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val authRepository: AuthRepository,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = authRepository.idToken.value
            ?: return chain.proceed(chain.request())

        if (JwtExpiry.isExpired(token)) {
            authRepository.onUnauthorized()
            return chain.proceed(chain.request())
        }

        val authorized = chain.request()
            .newBuilder()
            .addHeader(AUTHORIZATION_HEADER, "$BEARER_PREFIX$token")
            .build()

        val response = chain.proceed(authorized)

        if (response.code == HTTP_UNAUTHORIZED) {
            authRepository.onUnauthorized()
        }

        return response
    }

    private companion object {
        const val AUTHORIZATION_HEADER = "Authorization"

        // Esquema estándar para un token de Google, y el que espera la política de
        // validación de JWT del APIM.
        // TODO: confirmar con Cabrera que el APIM lee este encabezado y no uno propio.
        const val BEARER_PREFIX = "Bearer "

        const val HTTP_UNAUTHORIZED = 401
    }
}
