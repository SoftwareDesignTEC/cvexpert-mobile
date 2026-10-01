package com.cvpro.app.domain.repository

import com.cvpro.app.domain.model.ActiveSession
import com.cvpro.app.domain.model.LegalDocument

interface SettingsContentRepository {

    suspend fun getSessions(
        userId: String,
    ): List<ActiveSession>

    suspend fun getPrivacyPolicy(): LegalDocument

    suspend fun getTerms(): LegalDocument

    suspend fun exportUserData(
        userId: String,
    ): String
}