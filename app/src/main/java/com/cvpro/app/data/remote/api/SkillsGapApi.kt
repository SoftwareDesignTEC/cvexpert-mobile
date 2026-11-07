package com.cvpro.app.data.remote.api

import com.cvpro.app.data.remote.dto.SkillsGapPageDto
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Query

interface SkillsGapApi {
    @GET("skills-gap")
    suspend fun getSkillsGap(
        @Query("page") page: Long,
        @Query("pageSize") pageSize: Int,
    ): Response<SkillsGapPageDto>

    @PUT("skills-gap/input")
    suspend fun putInput(@Body input: RequestBody): Response<Unit>
}
