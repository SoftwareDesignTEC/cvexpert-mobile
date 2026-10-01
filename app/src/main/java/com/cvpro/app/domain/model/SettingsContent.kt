package com.cvpro.app.domain.model

data class ActiveSession(
    val id: String,
    val deviceName: String,
    val platform: String,
    val client: String,
    val lastActiveAt: String,
    val current: Boolean,
)

data class LegalDocument(
    val id: String,
    val title: String,
    val version: String,
    val effectiveDate: String,
    val content: String,
)