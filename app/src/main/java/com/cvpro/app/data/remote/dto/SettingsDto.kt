package com.cvpro.app.data.remote.dto

data class ActiveSessionDto(
    val id: String,
    val deviceName: String,
    val platform: String,
    val client: String,
    val lastActiveAt: String,
    val current: Boolean,
)

data class LegalDocumentDto(
    val id: String,
    val title: String,
    val version: String,
    val effectiveDate: String,
    val content: String,
)