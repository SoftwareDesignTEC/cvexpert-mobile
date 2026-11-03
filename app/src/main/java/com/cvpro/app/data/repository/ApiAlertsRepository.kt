package com.cvpro.app.data.repository

import com.cvpro.app.data.mapper.toDomain
import com.cvpro.app.data.mapper.toRequest
import com.cvpro.app.data.remote.api.AlertsApi
import com.cvpro.app.domain.model.JobAlertSettings
import com.cvpro.app.domain.model.UserProfile
import com.cvpro.app.domain.repository.AlertsRepository
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiAlertsRepository @Inject constructor(
    private val api: AlertsApi,
) : AlertsRepository {

    override suspend fun getAlerts(): JobAlertSettings? {
        return try {
            api.getAlerts().toDomain()
        } catch (e: HttpException) {
            // 404 no es un error: es alguien que todavía no configuró alertas.
            if (e.code() == HTTP_NOT_FOUND) null else throw e
        }
    }

    override suspend fun saveAlerts(settings: JobAlertSettings): JobAlertSettings {
        return api.saveAlerts(settings.toRequest()).toDomain()
    }

    override suspend fun syncProfile(profile: UserProfile) {
        val current = getAlerts() ?: return
        val synced = current.copy(
            location = profile.location,
            modalities = profile.modalities,
            jobTypes = profile.types,
            sectors = profile.sectors,
        )
        // Si el perfil no cambió nada de lo que usan las alertas, no hace falta llamar.
        if (synced != current) saveAlerts(synced)
    }

    private companion object {
        const val HTTP_NOT_FOUND = 404
    }
}
