package com.cvpro.app.domain.model

/**
 * Una página de resultados.
 *
 * Los nombres son los que exige el enunciado para los mocks del APIM: items, page, pageSize
 * y totalRecords. Al conectar el servicio, la respuesta del mock se convierte a este mismo
 * tipo y las pantallas no cambian.
 */
data class PagedResult<T>(
    val items: List<T>,
    val page: Int,
    val pageSize: Int,
    val totalRecords: Int,
) {
    /** True si todavía quedan páginas por pedir. */
    val hasMore: Boolean = page * pageSize < totalRecords
}
