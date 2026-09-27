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
        val baseUrl = BuildConfig.APIM_BASE_URL
            .trim()
            .let { url ->
                if (url.endsWith("/")) url else "$url/"
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

}