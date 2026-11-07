package com.cvpro.app.data.remote.api

import com.cvpro.app.data.remote.dto.CvTemplateDto
import com.cvpro.app.data.remote.dto.PagedResponseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TemplateApi {

    @GET("templates")
    suspend fun getTemplates(
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int,
        @Query("search") search: String?,
    ): PagedResponseDto<CvTemplateDto>

    /**
     * Una plantilla por id, mismo patrón que `GET /cvs/{id}` (lo agregó Pérez en la rama
     * feat/get-template-by-id). 404 si no existe.
     * TODO: confirmar con Pérez/Cabrera cuándo queda mergeado y publicado en el APIM.
     */
    @GET("templates/{id}")
    suspend fun getTemplateById(
        @Path("id") id: String,
    ): CvTemplateDto
}