package com.cvpro.app.domain.repository

import com.cvpro.app.domain.model.JobOffer
import com.cvpro.app.domain.model.PagedResult

/**
 * Fuente de las ofertas laborales.
 *
 * La pantalla solo conoce esta interfaz. Hoy la implementa una fuente de datos de prueba;
 * cuando existan los mocks del APIM se cambia la implementación y la pantalla queda igual.
 */
interface JobOfferRepository {

    /**
     * Devuelve una página de ofertas.
     *
     * @param page número de página, empezando en 1.
     * @param query texto de búsqueda; vacío devuelve todas.
     */
    suspend fun getOffers(page: Int, pageSize: Int = DEFAULT_PAGE_SIZE, query: String = ""): PagedResult<JobOffer>

    companion object {
        const val DEFAULT_PAGE_SIZE = 10
    }
}
