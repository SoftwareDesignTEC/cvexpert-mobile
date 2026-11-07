package com.cvpro.app.data.repository

import com.cvpro.app.data.mapper.toDomain
import com.cvpro.app.data.remote.api.TemplateApi
import com.cvpro.app.domain.model.CvTemplate
import com.cvpro.app.domain.model.PagedResult
import com.cvpro.app.domain.repository.TemplateRepository
import java.util.concurrent.ConcurrentHashMap
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiTemplateRepository @Inject constructor(
    private val api: TemplateApi,
) : TemplateRepository {

    // Plantillas que ya llegaron del servicio, por id. El catálogo casi no cambia y así el PDF
    // no vuelve a pedirlo cada vez. Se pierde al cerrar la app, que es suficiente.
    private val cache = ConcurrentHashMap<String, CvTemplate>()

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

        val items = response.items.map { it.toDomain() }
        items.forEach { cache[it.id] = it }

        return PagedResult(
            items = items,
            page = response.page,
            pageSize = response.pageSize,
            totalRecords = response.totalRecords,
        )
    }

    /**
     * Pide la plantilla con `GET /templates/{id}`. Mientras esa ruta no esté publicada en el
     * APIM responde 404; en ese caso se busca en el catálogo como antes, así la app funciona
     * con o sin la ruta nueva. Un 404 del catálogo también significa "no existe": devuelve null.
     */
    override suspend fun getTemplate(id: String): CvTemplate? {
        cache[id]?.let { return it }
        try {
            return api.getTemplateById(id).toDomain().also { cache[it.id] = it }
        } catch (exception: HttpException) {
            if (exception.code() != HTTP_NOT_FOUND) throw exception
        }
        return findInCatalog(id)
    }

    /**
     * `search` filtra por nombre, no por id, así que se recorre el catálogo página por página
     * hasta encontrarla. Son pocas plantillas.
     */
    private suspend fun findInCatalog(id: String): CvTemplate? {
        var page = 1
        while (page <= MAX_PAGES) {
            val result = getTemplates(page = page, pageSize = PAGE_SIZE, query = "")
            cache[id]?.let { return it }
            if (page * PAGE_SIZE >= result.totalRecords || result.items.isEmpty()) return null
            page++
        }
        return null
    }

    private companion object {
        const val HTTP_NOT_FOUND = 404
        const val PAGE_SIZE = 50

        // Tope por si el servicio manda un totalRecords equivocado: nunca más de 500 plantillas.
        const val MAX_PAGES = 10
    }
}
