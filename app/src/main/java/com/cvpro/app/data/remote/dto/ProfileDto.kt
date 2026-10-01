package com.cvpro.app.data.remote.dto

data class UserProfileDto(
    val id: String? = null,
    val name: String? = null,
    val email: String? = null,
    val phone: String?,
    val location: String? = null,
    val photoUrl: String?,
    val bio: String?,
    val birthDate: String?,
    val types: List<String>? = null,
    val modalities: List<String>? = null,
    val sectors: List<String>? = null,
)

data class UpdateProfileRequestDto(
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val location: String? = null,
    val photoUrl: String? = null,
    val bio: String? = null,
    val birthDate: String? = null,
    val types: List<String>? = null,
    val modalities: List<String>? = null,
    val sectors: List<String>? = null,
)