package com.cvpro.app.data.repository

import com.cvpro.app.data.mapper.newCvRequest
import com.cvpro.app.data.mapper.toDomain
import com.cvpro.app.data.mapper.toUpdateRequest
import com.cvpro.app.data.remote.api.CvApi
import com.cvpro.app.domain.model.Cv
import com.cvpro.app.domain.model.PagedResult
import com.cvpro.app.domain.model.UserProfile
import com.cvpro.app.domain.repository.CvRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiCvRepository @Inject constructor(
    private val api: CvApi,
) : CvRepository {

    override suspend fun getCvs(
        page: Int,
        pageSize: Int,
        query: String,
    ): PagedResult<Cv> {

        val response = api.getCvs(
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

    override suspend fun getCvById(id: String): Cv {
        return api.getCvById(id).toDomain()
    }

    /**
     * Manda el currículum completo con PUT.
     *
     * El mock devuelve lo que acaba de recibir, así que la pantalla se queda con la
     * respuesta y no con su copia local: si el día de mañana el servicio real cambia algo
     * al guardar (una fecha de modificación, por ejemplo), se ve sin tocar la app.
     */
    override suspend fun updateCv(cv: Cv): Cv {
        return api.updateCv(id = cv.id, request = cv.toUpdateRequest()).toDomain()
    }

    override suspend fun createCv(
        title: String,
        templateId: String,
        personalInfo: UserProfile?,
    ): Cv {
        return api.createCv(
            newCvRequest(
                title = title,
                templateId = templateId,
                personalInfo = personalInfo,
            ),
        ).toDomain()
    }

    override suspend fun deleteCv(id: String) {
        api.deleteCv(id)
    }
}