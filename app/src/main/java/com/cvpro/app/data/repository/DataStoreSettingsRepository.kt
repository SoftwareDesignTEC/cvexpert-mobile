package com.cvpro.app.data.repository

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.cvpro.app.domain.model.ThemePreference
import com.cvpro.app.domain.repository.NotificationSettings
import com.cvpro.app.domain.repository.NotificationType
import com.cvpro.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Guarda las preferencias con DataStore.
 *
 * El tema se guarda como texto (el nombre del enum). Si el archivo se corrompe o el valor
 * guardado ya no existe, se vuelve al valor por defecto en vez de reventar.
 */
@Singleton
class DataStoreSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override val themePreference: Flow<ThemePreference> = dataStore.data
        .catch { error ->
            // Si no se puede leer el archivo, la app sigue con el tema del sistema.
            if (error is IOException) {
                Log.w(TAG, "No se pudieron leer las preferencias", error)
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map { preferences ->
            val stored = preferences[THEME_KEY]
            ThemePreference.entries.firstOrNull { it.name == stored } ?: ThemePreference.SYSTEM
        }

    override suspend fun setThemePreference(preference: ThemePreference) {
        dataStore.edit { preferences ->
            preferences[THEME_KEY] = preference.name
        }
    }

    override val notifications: Flow<NotificationSettings> = dataStore.data
        .catch { error ->
            if (error is IOException) {
                Log.w(TAG, "No se pudieron leer las preferencias de avisos", error)
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map { preferences ->
            // Si nunca se guardó nada, los tres avisos quedan encendidos.
            NotificationSettings(
                applicationStatus = preferences[keyOf(NotificationType.APPLICATION_STATUS)] ?: true,
                courseRecommendation = preferences[keyOf(NotificationType.COURSE_RECOMMENDATION)] ?: true,
                jobAlerts = preferences[keyOf(NotificationType.JOB_ALERTS)] ?: true,
            )
        }

    override suspend fun setNotification(type: NotificationType, enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[keyOf(type)] = enabled
        }
    }

    private companion object {
        const val TAG = "SettingsRepository"
        val THEME_KEY = stringPreferencesKey("theme_preference")

        /** Una clave por tipo de aviso, derivada del nombre del enum. */
        fun keyOf(type: NotificationType) = booleanPreferencesKey("notify_${type.name.lowercase()}")
    }
}
