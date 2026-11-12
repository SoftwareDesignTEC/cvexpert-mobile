package com.cvpro.app.di

import com.cvpro.app.BuildConfig
import com.cvpro.app.data.remote.AuthInterceptor
import com.cvpro.app.data.remote.LoggingInterceptor
import com.cvpro.app.data.remote.api.JobApi
import com.cvpro.app.data.remote.api.NotificationApi
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton
import com.cvpro.app.data.remote.api.AlertsApi
import com.cvpro.app.data.remote.api.ApplicationApi
import com.cvpro.app.data.remote.api.ProfileApi
import com.cvpro.app.data.remote.api.SkillsGapApi
import com.cvpro.app.data.remote.api.TemplateApi
import com.cvpro.app.data.remote.api.CvApi
import com.cvpro.app.data.remote.api.SettingsApi

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideCourseApi(retrofit: Retrofit): com.cvpro.app.data.remote.api.CourseApi =
        retrofit.create(com.cvpro.app.data.remote.api.CourseApi::class.java)


    /**
     * Cliente HTTP con el token de Google adjunto y tiempos de espera propios.
     *
     * Sin tiempos de espera, una llamada que no responde deja la pantalla cargando para
     * siempre en vez de mostrar el error.
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        loggingInterceptor: LoggingInterceptor,
    ): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .addInterceptor(aiTimeoutInterceptor)
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit {
        // Si la URL del APIM todavía no está configurada (local.properties vacío o el
        // workflow sin el secret), Retrofit lanzaría IllegalArgumentException al construirse
        // y la app se caería al abrir cualquier pantalla con datos. Con este reemplazo la
        // app arranca igual y las pantallas muestran su estado de error con "Reintentar".
        val configuredUrl = BuildConfig.APIM_BASE_URL.trim()
        val baseUrl = when {
            configuredUrl.isEmpty() -> PLACEHOLDER_BASE_URL
            configuredUrl.endsWith("/") -> configuredUrl
            else -> "$configuredUrl/"
        }

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideJobApi(retrofit: Retrofit): JobApi {
        return retrofit.create(JobApi::class.java)
    }

    @Provides
    @Singleton
    fun provideApplicationApi(retrofit: Retrofit): ApplicationApi {
        return retrofit.create(ApplicationApi::class.java)
    }

    @Provides
    @Singleton
    fun provideProfileApi(retrofit: Retrofit): ProfileApi {
        return retrofit.create(ProfileApi::class.java)
    }

    @Provides
    @Singleton
    fun provideTemplateApi(retrofit: Retrofit): TemplateApi {
        return retrofit.create(TemplateApi::class.java)
    }

    @Provides
    @Singleton
    fun provideCvApi(retrofit: Retrofit): CvApi {
        return retrofit.create(CvApi::class.java)
    }

    @Provides
    @Singleton
    fun provideSkillsGapApi(retrofit: Retrofit, client: OkHttpClient): SkillsGapApi {
        // No automatic connection retries for analysis POSTs. Explicit UI retry repeats only the failed operation.
        // The cloned client preserves authentication, privacy logging, timeouts and the existing base URL.
        val skillsClient = client.newBuilder().retryOnConnectionFailure(false).build()
        return retrofit.newBuilder().client(skillsClient).build().create(SkillsGapApi::class.java)
    }

    @Provides
    @Singleton
    fun provideSettingsApi(retrofit: Retrofit, ): SettingsApi {
        return retrofit.create(SettingsApi::class.java)
    }

    @Provides
    @Singleton
    fun provideNotificationApi(retrofit: Retrofit): NotificationApi {
        return retrofit.create(NotificationApi::class.java)
    }

    @Provides
    @Singleton
    fun provideAlertsApi(retrofit: Retrofit): AlertsApi {
        return retrofit.create(AlertsApi::class.java)
    }

    /**
     * URL de relleno para cuando APIM_BASE_URL está vacía.
     *
     * No apunta a ningún servicio real: solo existe para que Retrofit acepte construirse.
     * Toda llamada falla y la pantalla muestra su estado de error, que es el comportamiento
     * correcto cuando no hay servicio configurado.
     */
    private const val PLACEHOLDER_BASE_URL = "https://apim-sin-configurar.invalid/"

    /** Tiempo máximo de espera de una llamada, en segundos. */
    private const val TIMEOUT_SECONDS = 20L

    // Lo que puede tardar la IA en redactar una respuesta. El APIM corta las rutas de IA a los
    // 120 s; la app espera un poco más para que, si algo se corta, sea el APIM con su error y
    // no la app con un "sin conexión" que confunde.
    private const val AI_TIMEOUT_SECONDS = 130L

    // Modo IA: arma un CV entero de una vez. El APIM le da 180 s a generate-cv (confirmado por
    // Cabrera); la app espera 190.
    private const val AI_CV_TIMEOUT_SECONDS = 190L

    /**
     * Le da más tiempo de espera solo a las rutas de IA del servicio de CVs (/cvs/ai/...).
     * Así no hace falta un segundo cliente HTTP, y el resto de la app sigue cortando a los
     * 20 segundos para mostrar el error rápido.
     */
    private val aiTimeoutInterceptor = Interceptor { chain ->
        val path = chain.request().url.encodedPath
        if (path.contains("/cvs/ai/")) {
            val seconds = if (path.endsWith("/generate-cv")) AI_CV_TIMEOUT_SECONDS else AI_TIMEOUT_SECONDS
            chain.withReadTimeout(seconds.toInt(), TimeUnit.SECONDS)
                .proceed(chain.request())
        } else {
            chain.proceed(chain.request())
        }
    }
}
