package com.cvpro.app.domain.repository

import com.cvpro.app.domain.model.ApplicationStatus
import com.cvpro.app.domain.model.JobApplication
import com.cvpro.app.domain.model.JobModality
import com.cvpro.app.domain.model.PagedResult

interface ApplicationRepository {

    suspend fun getApplications(
        page: Int,
        pageSize: Int,
        query: String,
    ): PagedResult<JobApplication>

    suspend fun createApplication(
        userId: String,
        jobOfferId: String,
        jobTitle: String,
        company: String,
        companyLogoUrl: String?,
        location: String,
        modality: JobModality,
        status: ApplicationStatus,
        notes: String?,
    ): JobApplication

    suspend fun updateApplicationStatus(
        id: String,
        status: ApplicationStatus,
    ): JobApplication
}