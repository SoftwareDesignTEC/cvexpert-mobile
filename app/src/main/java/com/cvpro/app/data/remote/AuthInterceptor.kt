package com.cvpro.app.data.remote

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
 * Si todavía no hay sesión, la petición sale sin el encabezado: el servicio responderá 401 y
 * la pantalla mostrará su estado de error, que es el comportamiento correcto.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val authRepository: AuthRepository,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = authRepository.idToken.value
            ?: return chain.proceed(chain.request())

        val authorized = chain.request()
            .newBuilder()
            .addHeader(AUTHORIZATION_HEADER, "$BEARER_PREFIX$token")
            .build()

        return chain.proceed(authorized)
    }

    private companion object {
        const val AUTHORIZATION_HEADER = "Authorization"

        // Esquema estándar para un token de Google, y el que espera la política de
        // validación de JWT del APIM.
        // TODO: confirmar con Cabrera que el APIM lee este encabezado y no uno propio.
        const val BEARER_PREFIX = "Bearer "
    }
}
