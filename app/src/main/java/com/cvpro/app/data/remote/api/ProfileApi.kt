package com.cvpro.app.data.remote.api

import com.cvpro.app.data.remote.dto.UpdateProfileRequestDto
import com.cvpro.app.data.remote.dto.UserProfileDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

interface ProfileApi {

    @GET("profiles/{id}")
    suspend fun getProfile(
        @Path("id") id: String,
    ): UserProfileDto

    @PUT("profiles/{id}")
    suspend fun updateProfile(
        @Path("id") id: String,
        @Body request: UpdateProfileRequestDto,
    ): UserProfileDto
}