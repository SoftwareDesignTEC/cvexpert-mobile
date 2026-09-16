package com.cvpro.app.data.repository

import com.cvpro.app.domain.repository.AuthRepository
import com.cvpro.app.domain.repository.JobOfferRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

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

    // TODO: cambiar por la implementación contra el APIM cuando existan los mocks.
    @Binds
    @Singleton
    abstract fun bindJobOfferRepository(impl: SampleJobOfferRepository): JobOfferRepository
}
