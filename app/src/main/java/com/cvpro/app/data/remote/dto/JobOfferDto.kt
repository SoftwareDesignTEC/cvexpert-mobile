package com.cvpro.app.data.remote.dto

data class JobOfferDto(
    val id: String? = null,
    val title: String? = null,
    val company: String? = null,
    val companyLogoUrl: String?,
    val location: String? = null,
    val modality: String? = null,
    val type: String? = null,
    val sector: String? = null,
    val description: String? = null,
    val requirements: List<String>? = null,
    val salary: SalaryDto?,
    val matchScore: Int?,
    val postedAt: String? = null,
)

data class SalaryDto(
    val min: Double? = null,
    val max: Double? = null,
    val currency: String? = null,
)