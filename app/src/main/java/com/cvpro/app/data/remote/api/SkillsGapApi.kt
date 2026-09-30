package com.cvpro.app.data.remote.api

import com.cvpro.app.data.remote.dto.PagedResponseDto
import com.cvpro.app.data.remote.dto.SkillsGapDto
import retrofit2.http.GET
import retrofit2.http.Query

interface SkillsGapApi {

    @GET("skills-gap")
    suspend fun getSkillsGap(
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int,
    ): PagedResponseDto<SkillsGapDto>
}