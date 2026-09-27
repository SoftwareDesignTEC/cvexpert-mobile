package com.cvpro.app.data.remote.api

import com.cvpro.app.data.remote.dto.JobOfferDto
import com.cvpro.app.data.remote.dto.PagedResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface JobApi {

    @GET("jobs")
    suspend fun getJobs(
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int,
        @Query("search") search: String?,
    ): PagedResponseDto<JobOfferDto>
}