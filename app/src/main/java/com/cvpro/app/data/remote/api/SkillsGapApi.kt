package com.cvpro.app.data.remote.api

import com.cvpro.app.data.remote.dto.SkillsGapPageDto
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface SkillsGapApi {
    @GET("skills-gap")
    suspend fun getSkillsGap(@Query("page") page: Int, @Query("pageSize") pageSize: Int): Response<SkillsGapPageDto>
    @PUT("skills-gap/input")
    suspend fun putInput(@Body input: RequestBody): Response<Unit>
}
