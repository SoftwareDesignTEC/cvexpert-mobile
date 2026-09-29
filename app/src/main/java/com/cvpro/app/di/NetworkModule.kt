package com.cvpro.app.di

import com.cvpro.app.BuildConfig
import com.cvpro.app.data.remote.api.JobApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton
import com.cvpro.app.data.remote.api.ApplicationApi
import com.cvpro.app.data.remote.api.ProfileApi
import com.cvpro.app.data.remote.api.TemplateApi
import com.cvpro.app.data.remote.api.CvApi

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit {
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

    /**
     * URL de relleno para cuando APIM_BASE_URL está vacía.
     *
     * No apunta a ningún servicio real: solo existe para que Retrofit acepte construirse.
     * Toda llamada falla y la pantalla muestra su estado de error, que es el comportamiento
     * correcto cuando no hay servicio configurado.
     */
    private const val PLACEHOLDER_BASE_URL = "https://apim-sin-configurar.invalid/"
}