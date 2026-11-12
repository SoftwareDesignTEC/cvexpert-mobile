package com.cvpro.app.data.remote.api

import com.cvpro.app.data.remote.dto.SkillsGapPageDto
import com.cvpro.app.data.remote.dto.JobSkillsGapEvaluationRequestDto
import com.cvpro.app.data.remote.dto.JobSkillsGapRequestDto
import com.cvpro.app.data.remote.dto.JobSkillsGapResponseDto
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface SkillsGapApi {
    @POST("skills-gap/recommendations")
    suspend fun recommendForJob(@Body input: JobSkillsGapRequestDto): Response<JobSkillsGapResponseDto>
    @POST("skills-gap/evaluate")
    suspend fun evaluateForJob(@Body input: JobSkillsGapEvaluationRequestDto): Response<JobSkillsGapResponseDto>
    @GET("skills-gap")
    suspend fun getSkillsGap(@Query("page") page: Int, @Query("pageSize") pageSize: Int): Response<SkillsGapPageDto>
    @PUT("skills-gap/input")
    suspend fun putInput(@Body input: RequestBody): Response<Unit>
}
