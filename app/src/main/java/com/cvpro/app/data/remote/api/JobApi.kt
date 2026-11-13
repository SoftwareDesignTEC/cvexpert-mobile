package com.cvpro.app.data.remote.api

import com.cvpro.app.data.remote.dto.JobOfferDto
import com.cvpro.app.data.remote.dto.PagedResponseDto
import com.cvpro.app.data.remote.dto.RefreshJobsDto
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface JobApi {
    @GET("jobs/{id}")
    suspend fun getJobById(@Path("id") id: String): JobOfferDto


    @GET("jobs")
    suspend fun getJobs(
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int,
        @Query("search") search: String?,
    ): PagedResponseDto<JobOfferDto>

    /**
     * Le pide al servicio que busque ofertas nuevas en JSearch ahora mismo. Responde 429 si se
     * pidió hace menos de 10 minutos.
     */
    @POST("jobs/refresh")
    suspend fun refreshJobs(): RefreshJobsDto
}
