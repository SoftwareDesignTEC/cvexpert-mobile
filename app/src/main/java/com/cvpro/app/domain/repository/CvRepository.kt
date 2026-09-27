package com.cvpro.app.domain.repository

import com.cvpro.app.domain.model.Cv
import com.cvpro.app.domain.model.PagedResult

interface CvRepository {

    suspend fun getCvs(
        page: Int,
        pageSize: Int,
        query: String,
    ): PagedResult<Cv>

    suspend fun getCvById(id: String): Cv

    suspend fun deleteCv(id: String)
}