package com.cvpro.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Crea el archivo de preferencias de la app y lo entrega a quien lo pida.
 *
 * DataStore guarda pares clave-valor en un archivo del almacenamiento privado de la app.
 * Acá solo se guardan preferencias (el tema), no datos del usuario.
 */
@Module
@InstallIn(SingletonComponent::class)
object PreferencesDataStoreModule {

    private const val PREFERENCES_FILE = "cvpro_preferences"

    @Provides
    @Singleton
    fun providePreferencesDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        produceFile = { context.preferencesDataStoreFile(PREFERENCES_FILE) },
    )
}
