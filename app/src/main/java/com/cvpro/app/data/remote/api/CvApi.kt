package com.cvpro.app.data.remote.api

import com.cvpro.app.data.remote.dto.CreateCvRequestDto
import com.cvpro.app.data.remote.dto.CvDto
import com.cvpro.app.data.remote.dto.PagedResponseDto
import com.cvpro.app.data.remote.dto.UpdateCvRequestDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface CvApi {

    @GET("cvs")
    suspend fun getCvs(
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int,
        @Query("search") search: String?,
    ): PagedResponseDto<CvDto>

    @GET("cvs/{id}")
    suspend fun getCvById(
        @Path("id") id: String,
    ): CvDto

    @POST("cvs")
    suspend fun createCv(
        @Body request: CreateCvRequestDto,
    ): CvDto

    @PUT("cvs/{id}")
    suspend fun updateCv(
        @Path("id") id: String,
        @Body request: UpdateCvRequestDto,
    ): CvDto

    @DELETE("cvs/{id}")
    suspend fun deleteCv(
        @Path("id") id: String,
    )
}