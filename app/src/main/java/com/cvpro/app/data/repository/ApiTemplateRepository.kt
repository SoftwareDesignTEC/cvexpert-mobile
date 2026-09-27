package com.cvpro.app.data.repository

import com.cvpro.app.data.mapper.toDomain
import com.cvpro.app.data.remote.api.TemplateApi
import com.cvpro.app.domain.model.CvTemplate
import com.cvpro.app.domain.model.PagedResult
import com.cvpro.app.domain.repository.TemplateRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiTemplateRepository @Inject constructor(
    private val api: TemplateApi,
) : TemplateRepository {

    override suspend fun getTemplates(
        page: Int,
        pageSize: Int,
        query: String,
    ): PagedResult<CvTemplate> {

        val response = api.getTemplates(
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