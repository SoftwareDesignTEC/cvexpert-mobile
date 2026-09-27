package com.cvpro.app.data.remote.dto

data class CvTemplateDto(
    val id: String,
    val name: String,
    val description: String,
    val previewUrl: String,
    val category: String,
    val isPremium: Boolean,
    val availableOffline: Boolean,
)