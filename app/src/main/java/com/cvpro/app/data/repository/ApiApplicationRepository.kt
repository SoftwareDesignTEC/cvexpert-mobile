package com.cvpro.app.data.repository

import com.cvpro.app.data.mapper.toDomain
import com.cvpro.app.data.remote.api.ApplicationApi
import com.cvpro.app.data.remote.dto.CreateApplicationRequestDto
import com.cvpro.app.data.remote.dto.UpdateApplicationStatusRequestDto
import com.cvpro.app.domain.model.ApplicationStatus
import com.cvpro.app.domain.model.JobApplication
import com.cvpro.app.domain.model.JobModality
import com.cvpro.app.domain.model.PagedResult
import com.cvpro.app.domain.repository.ApplicationRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiApplicationRepository @Inject constructor(
    private val api: ApplicationApi,
) : ApplicationRepository {

    override suspend fun getApplications(
        page: Int,
        pageSize: Int,
        query: String,
    ): PagedResult<JobApplication> {

        val response = api.getApplications(
            page = page,
            pageSize = pageSize,
            search = query.trim().takeIf { it.isNotEmpty() },
        )

        return PagedResult(
            items = response.items.map { it.toDomain() },
            page = response.page,
            pageSize = response.pageSize,
            totalRecords = response.totalRecords,
        )
    }

    override suspend fun createApplication(
        userId: String,
        jobOfferId: String,
        jobTitle: String,
        company: String,
        companyLogoUrl: String?,
        location: String,
        modality: JobModality,
        status: ApplicationStatus,
        notes: String?,
    ): JobApplication {

        val request = CreateApplicationRequestDto(
            userId = userId,
            jobOfferId = jobOfferId,
            jobTitle = jobTitle,
            company = company,
            companyLogoUrl = companyLogoUrl,
            location = location,
            modality = modality.toApiValue(),
            status = status.toApiValue(),
            notes = notes,
        )

        return api.createApplication(request).toDomain()
    }

    override suspend fun updateApplicationStatus(
        id: String,
        status: ApplicationStatus,
    ): JobApplication {

        val request = UpdateApplicationStatusRequestDto(
            status = status.toApiValue(),
        )

        return api.updateApplicationStatus(
            id = id,
            request = request,
        ).toDomain()
    }
}

private fun JobModality.toApiValue(): String {
    return when (this) {
        JobModality.REMOTE -> "remote"
        JobModality.ON_SITE -> "on-site"
        JobModality.HYBRID -> "hybrid"
    }
}

private fun ApplicationStatus.toApiValue(): String {
    return when (this) {
        ApplicationStatus.APPLIED -> "applied"
        ApplicationStatus.IN_PROGRESS -> "in-progress"
        ApplicationStatus.INTERVIEW -> "interview"
        ApplicationStatus.REJECTED -> "rejected"
    }
}