package com.cvpro.app.data.remote.dto

data class UserProfileDto(
    val id: String,
    val fullName: String,
    val email: String,
    val phone: String?,
    val address: String,
    val photoUrl: String?,
    val bio: String?,
    val types: List<String>,
    val modalities: List<String>,
    val sectors: List<String>,
)

data class UpdateProfileRequestDto(
    val fullName: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val photoUrl: String? = null,
    val bio: String? = null,
    val types: List<String>? = null,
    val modalities: List<String>? = null,
    val sectors: List<String>? = null,
)