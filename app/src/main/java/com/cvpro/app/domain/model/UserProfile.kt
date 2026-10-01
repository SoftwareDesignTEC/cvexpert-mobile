package com.cvpro.app.domain.model

data class UserProfile(
    val id: String,
    val name: String,
    val email: String,
    val phone: String?,
    val location: String,
    val photoUrl: String?,
    val bio: String?,
    val types: List<String>,
    val modalities: List<JobModality>,
    val sectors: List<String>,
)