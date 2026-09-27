package com.cvpro.app.data.remote.api

import com.cvpro.app.data.remote.dto.CreateApplicationRequestDto
import com.cvpro.app.data.remote.dto.JobApplicationDto
import com.cvpro.app.data.remote.dto.PagedResponseDto
import com.cvpro.app.data.remote.dto.UpdateApplicationStatusRequestDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApplicationApi {

    @GET("applications")
    suspend fun getApplications(
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int,
        @Query("search") search: String?,
    ): PagedResponseDto<JobApplicationDto>

    @POST("applications")
    suspend fun createApplication(
        @Body application: CreateApplicationRequestDto,
    ): JobApplicationDto

    @PUT("applications/{id}")
    suspend fun updateApplicationStatus(
        @Path("id") id: String,
        @Body request: UpdateApplicationStatusRequestDto,
    ): JobApplicationDto
}