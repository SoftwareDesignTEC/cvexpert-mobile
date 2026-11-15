package com.cvpro.app.data.repository

import com.cvpro.app.data.mapper.toDomain
import com.cvpro.app.data.mapper.toWireValue
import com.cvpro.app.data.remote.api.ProfileApi
import com.cvpro.app.data.remote.dto.UpdateProfileRequestDto
import com.cvpro.app.domain.model.JobModality
import com.cvpro.app.domain.model.UserProfile
import com.cvpro.app.domain.repository.ProfileRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiProfileRepository @Inject constructor(
    private val api: ProfileApi,
) : ProfileRepository {

    override suspend fun getProfile(
        id: String,
    ): UserProfile {
        return api.getProfile(id).toDomain()
    }

    override suspend fun updateProfile(
        id: String,
        name: String,
        email: String,
        phone: String?,
        location: String,
        bio: String?,
        types: List<String>?,
        modalities: List<JobModality>?,
    ): UserProfile {

        val request = UpdateProfileRequestDto(
            fullName = name,
            email = email,
            phone = phone,
            address = location,
            bio = bio,
            types = types,
            // Mismos valores que manda la web: "remote", "on-site", "hybrid".
            modalities = modalities?.map { it.toWireValue() },
        )

        return api.updateProfile(
            id = id,
            request = request,
        ).toDomain()
    }
}
