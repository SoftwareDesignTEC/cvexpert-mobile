package com.cvpro.app.data.repository

import com.cvpro.app.data.mapper.toDomain
import com.cvpro.app.data.remote.api.JobApi
import com.cvpro.app.domain.model.JobOffer
import com.cvpro.app.domain.model.PagedResult
import com.cvpro.app.domain.repository.JobOfferRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiJobOfferRepository @Inject constructor(
    private val api: JobApi,
) : JobOfferRepository {

    override suspend fun getOffers(
        page: Int,
        pageSize: Int,
        query: String,
    ): PagedResult<JobOffer> {

        val response = api.getJobs(
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
}