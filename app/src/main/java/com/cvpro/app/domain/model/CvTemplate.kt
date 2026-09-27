package com.cvpro.app.domain.model

data class CvTemplate(
    val id: String,
    val name: String,
    val description: String,
    val previewUrl: String,
    val category: String,
    val isPremium: Boolean,
    val availableOffline: Boolean,
)