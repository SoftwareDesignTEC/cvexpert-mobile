package com.cvpro.app.domain.repository

import com.cvpro.app.domain.model.JobOffer
import com.cvpro.app.domain.model.PagedResult

/**
 * Fuente de las ofertas laborales.
 *
 * La pantalla solo conoce esta interfaz. Hoy la implementa una fuente de datos de prueba;
 * cuando existan los mocks del APIM se cambia la implementación y la pantalla queda igual.
 */
/** Resultado de buscar ofertas nuevas. */
data class RefreshedOffers(val created: Int, val updated: Int)

interface JobOfferRepository {

    /**
     * Devuelve una página de ofertas.
     *
     * @param page número de página, empezando en 1.
     * @param query texto de búsqueda; vacío devuelve todas.
     */
    suspend fun getOffers(page: Int, pageSize: Int = DEFAULT_PAGE_SIZE, query: String = ""): PagedResult<JobOffer>

    /**
     * Busca ofertas nuevas en la fuente real (JSearch) en este momento, sin esperar a la
     * importación programada. Devuelve cuántas entraron nuevas y cuántas se actualizaron.
     *
     * Por defecto no está soportado: solo la fuente real (el servicio de Ofertas) sabe hacerlo,
     * y así las fuentes de prueba no tienen que implementarlo.
     */
    suspend fun refreshOffers(): RefreshedOffers =
        throw UnsupportedOperationException("Esta fuente no busca ofertas nuevas")

    companion object {
        const val DEFAULT_PAGE_SIZE = 10
    }
}
