package com.cvpro.app.data.remote.api

import com.cvpro.app.data.remote.dto.CvTemplateDto
import com.cvpro.app.data.remote.dto.PagedResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface TemplateApi {

    @GET("templates")
    suspend fun getTemplates(
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int,
        @Query("search") search: String?,
    ): PagedResponseDto<CvTemplateDto>
}