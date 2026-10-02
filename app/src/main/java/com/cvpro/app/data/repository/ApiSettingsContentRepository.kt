package com.cvpro.app.data.repository

import com.cvpro.app.data.mapper.toDomain
import com.cvpro.app.data.remote.api.SettingsApi
import com.cvpro.app.domain.model.ActiveSession
import com.cvpro.app.domain.model.HelpContact
import com.cvpro.app.domain.model.LegalDocument
import com.cvpro.app.domain.repository.SettingsContentRepository
import javax.inject.Inject

class ApiSettingsContentRepository @Inject constructor(
    private val api: SettingsApi,
) : SettingsContentRepository {

    override suspend fun getSessions(
        userId: String,
    ): List<ActiveSession> =
        api.getSessions(userId)
            .items
            .map { it.toDomain() }

    override suspend fun getPrivacyPolicy(): LegalDocument =
        api.getPrivacyPolicy().toDomain()

    override suspend fun getTerms(): LegalDocument =
        api.getTerms().toDomain()

    override suspend fun getHelpContacts(): List<HelpContact> =
        api.getHelpContacts()
            .contacts
            .orEmpty()
            .map { it.toDomain() }
            // Sin correo el botón no puede abrir nada, así que ese contacto no se muestra.
            .filter { it.email.isNotBlank() }

    override suspend fun exportUserData(
        userId: String,
    ): String =
        api.exportUserData(userId).string()
}