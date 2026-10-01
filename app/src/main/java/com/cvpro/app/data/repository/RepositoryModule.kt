package com.cvpro.app.data.repository

import com.cvpro.app.domain.repository.AuthRepository
import com.cvpro.app.domain.repository.JobOfferRepository
import com.cvpro.app.domain.repository.SettingsRepository
import com.cvpro.app.domain.repository.SkillsGapRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.cvpro.app.domain.repository.ApplicationRepository
import com.cvpro.app.domain.repository.ProfileRepository
import com.cvpro.app.domain.repository.TemplateRepository
import com.cvpro.app.domain.repository.CvRepository
import com.cvpro.app.domain.repository.SettingsContentRepository

/**
 * Le dice a Hilt qué implementación entregar cuando alguien pide una interfaz del dominio.
 *
 * Gracias a esto, un ViewModel pide `AuthRepository` en su constructor y Hilt le pasa
 * `GoogleAuthRepository` sin que el ViewModel sepa que existe.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: GoogleAuthRepository): AuthRepository

    @Binds
    @Singleton
    abstract fun bindJobOfferRepository(impl: ApiJobOfferRepository): JobOfferRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: DataStoreSettingsRepository): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindSkillsGapRepository(impl: ApiSkillsGapRepository): SkillsGapRepository

    @Binds
    @Singleton
    abstract fun bindApplicationRepository(impl: ApiApplicationRepository, ): ApplicationRepository

    @Binds
    @Singleton
    abstract fun bindProfileRepository(impl: ApiProfileRepository, ): ProfileRepository

    @Binds
    @Singleton
    abstract fun bindTemplateRepository(impl: ApiTemplateRepository, ): TemplateRepository

    @Binds
    @Singleton
    abstract fun bindCvRepository(
        impl: ApiCvRepository,
    ): CvRepository

    @Binds
    @Singleton
    abstract fun bindSettingsContentRepository(
        impl: ApiSettingsContentRepository,
    ): SettingsContentRepository
}
