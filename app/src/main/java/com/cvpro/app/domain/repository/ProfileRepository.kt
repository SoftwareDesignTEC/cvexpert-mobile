package com.cvpro.app.domain.repository

import com.cvpro.app.domain.model.UserProfile

interface ProfileRepository {

    suspend fun getProfile(
        id: String,
    ): UserProfile

    suspend fun updateProfile(
        id: String,
        name: String,
        email: String,
        phone: String?,
        location: String,
    ): UserProfile
}