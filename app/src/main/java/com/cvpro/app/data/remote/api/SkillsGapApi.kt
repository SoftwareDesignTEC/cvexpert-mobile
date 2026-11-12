package com.cvpro.app.data.remote.api

import com.cvpro.app.data.remote.dto.SkillsGapPageDto
import com.cvpro.app.data.remote.dto.JobGapEvaluationRequestDto
import com.cvpro.app.data.remote.dto.JobGapEvaluationResponseDto
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.POST
import retrofit2.http.Query

interface SkillsGapApi {
    @POST("skills-gap/evaluate")
    suspend fun evaluate(@Body input: JobGapEvaluationRequestDto): Response<JobGapEvaluationResponseDto>

    @GET("skills-gap")
    suspend fun getSkillsGap(
        @Query("page") page: Long,
        @Query("pageSize") pageSize: Int,
    ): Response<SkillsGapPageDto>

    @PUT("skills-gap/input")
    suspend fun putInput(@Body input: RequestBody): Response<Unit>
}
