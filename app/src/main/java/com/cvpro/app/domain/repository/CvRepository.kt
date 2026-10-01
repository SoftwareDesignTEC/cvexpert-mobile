package com.cvpro.app.domain.repository

import com.cvpro.app.domain.model.Cv
import com.cvpro.app.domain.model.PagedResult
import com.cvpro.app.domain.model.UserProfile

interface CvRepository {

    suspend fun getCvs(
        page: Int,
        pageSize: Int,
        query: String,
    ): PagedResult<Cv>

    suspend fun getCvById(id: String): Cv

    /** Guarda el currículum editado y devuelve el que confirmó el servicio. */
    suspend fun updateCv(cv: Cv): Cv

    /** Crea un currículum vacío a partir de una plantilla. */
    suspend fun createCv(
        title: String,
        templateId: String,
        personalInfo: UserProfile?,
    ): Cv

    suspend fun deleteCv(id: String)
}