package com.cvpro.app.data.remote.api

import com.cvpro.app.data.remote.dto.ActiveSessionDto
import com.cvpro.app.data.remote.dto.LegalDocumentDto
import com.cvpro.app.data.remote.dto.PagedResponseDto
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Path

interface SettingsApi {

    @GET("profiles/{id}/sessions")
    suspend fun getSessions(
        @Path("id") id: String,
    ): PagedResponseDto<ActiveSessionDto>

    @GET("legal/privacy")
    suspend fun getPrivacyPolicy(): LegalDocumentDto

    @GET("legal/terms")
    suspend fun getTerms(): LegalDocumentDto

    @GET("profiles/{id}/export")
    suspend fun exportUserData(
        @Path("id") id: String,
    ): ResponseBody
}