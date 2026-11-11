package com.cvpro.app.data.remote

import android.util.Log
import com.cvpro.app.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/** Diagnóstico de depuración limitado al código HTTP para todas las rutas. */
@Singleton
class LoggingInterceptor internal constructor(
    private val enabled: Boolean,
    private val log: (String) -> Unit,
) : Interceptor {

    @Inject constructor() : this(BuildConfig.DEBUG, { message -> Log.d(TAG, message) })

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (enabled) log("HTTP ${response.code}")
        return response
    }

    private companion object {
        const val TAG = "ApiResponse"
    }
}
