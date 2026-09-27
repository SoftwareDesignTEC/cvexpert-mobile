package com.cvpro.app.domain.repository

import com.cvpro.app.domain.model.CvTemplate
import com.cvpro.app.domain.model.PagedResult

interface TemplateRepository {

    suspend fun getTemplates(
        page: Int,
        pageSize: Int,
        query: String,
    ): PagedResult<CvTemplate>
}