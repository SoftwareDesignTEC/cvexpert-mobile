package com.cvpro.app.data.remote.dto

data class JobOfferDto(
    val id: String,
    val title: String,
    val company: String,
    val companyLogoUrl: String?,
    val location: String,
    val modality: String,
    val type: String,
    val sector: String,
    val description: String,
    val requirements: List<String>,
    val salary: SalaryDto?,
    val matchScore: Int?,
    val postedAt: String,
)

data class SalaryDto(
    val min: Double,
    val max: Double,
    val currency: String,
)