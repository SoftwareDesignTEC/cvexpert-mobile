package com.cvpro.app.domain.repository

import com.cvpro.app.domain.model.Cv
import com.cvpro.app.domain.model.CvLayout
import com.cvpro.app.domain.model.CvTemplate
import com.cvpro.app.domain.model.PagedResult
import kotlinx.coroutines.CancellationException

interface TemplateRepository {

    suspend fun getTemplates(
        page: Int,
        pageSize: Int,
        query: String,
    ): PagedResult<CvTemplate>

    /** La plantilla con ese id, o null si ya no está en el catálogo. */
    suspend fun getTemplate(id: String): CvTemplate?
}

/**
 * Cómo dibujar el PDF de un CV: el layout de su plantilla o, si la plantilla todavía no tiene
 * definición (`layout: null`), ya no existe o no se pudo pedir, el diseño de siempre.
 *
 * Que falle la consulta no impide el PDF: sin internet el CV se descarga igual, con
 * [CvLayout.DEFAULT].
 */
suspend fun TemplateRepository.layoutFor(cv: Cv): CvLayout =
    try {
        getTemplate(cv.templateId)?.layout ?: CvLayout.DEFAULT
    } catch (exception: CancellationException) {
        // Si se cerró la pantalla, se corta de verdad en vez de seguir con el diseño de siempre.
        throw exception
    } catch (exception: Exception) {
        CvLayout.DEFAULT
    }
