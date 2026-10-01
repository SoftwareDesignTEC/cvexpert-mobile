package com.cvpro.app.data.remote

import android.util.Log
import com.cvpro.app.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Escribe en el log lo que devuelve el servicio.
 *
 * Sirve para ver el JSON tal como llega cuando un campo no aparece en pantalla: sin esto
 * hay que adivinar qué nombre usa el mock. Solo corre en compilaciones de depuración, así
 * que el APK de release no registra nada.
 */
@Singleton
class LoggingInterceptor @Inject constructor() : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (!BuildConfig.DEBUG) return response

        // peekBody lee el cuerpo sin consumirlo: Retrofit lo sigue necesitando después.
        val body = response.peekBody(MAX_LOG_BYTES).string()
        Log.d(TAG, "${chain.request().url} → ${response.code}")
        Log.d(TAG, body)
        return response
    }

    private companion object {
        const val TAG = "ApiResponse"
        const val MAX_LOG_BYTES = 8_000L
    }
}
