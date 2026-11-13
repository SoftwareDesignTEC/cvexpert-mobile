package com.cvpro.app.data.pdf

import android.graphics.Bitmap
import android.util.LruCache
import com.cvpro.app.domain.model.Cv
import com.cvpro.app.domain.model.CvLayout
import com.cvpro.app.domain.model.CvTemplate
import com.cvpro.app.domain.repository.TemplateRepository
import kotlinx.coroutines.CancellationException
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Vistas previas "como foto" de currículums y plantillas: la primera hoja del PDF real,
 * dibujada por [CvPdfGenerator.renderPreview].
 *
 * La usan Panel Principal, Mis Currículums y Plantillas. Es @Singleton y guarda lo que ya
 * dibujó, porque cada pantalla recarga al mostrarse y no tiene sentido volver a dibujar ni a
 * pedirle al servicio el diseño de la plantilla cada vez.
 */
@Singleton
class CvPreviewRenderer @Inject constructor(
    private val pdfGenerator: CvPdfGenerator,
    private val templateRepository: TemplateRepository,
) {

    /**
     * Imágenes ya dibujadas, por currículum. La clave es el CV entero (es un data class): si
     * el usuario lo edita, cambia la clave y se dibuja de nuevo.
     */
    private val cvPreviews = LruCache<Cv, Bitmap>(MAX_CACHED_CVS)

    private val templatePreviews = ConcurrentHashMap<String, Bitmap>()

    /** Diseño de cada plantilla, por id, para no pedirlo una vez por currículum. */
    private val layouts = ConcurrentHashMap<String, CvLayout>()

    /** Vista previa de cada currículum, por id. Los que fallen simplemente no aparecen. */
    suspend fun previewsFor(cvs: List<Cv>): Map<String, Bitmap> =
        cvs.mapNotNull { cv -> preview(cv)?.let { cv.id to it } }.toMap()

    /** Vista previa de cada plantilla, por id, con el currículum de ejemplo. */
    suspend fun templatePreviewsFor(templates: List<CvTemplate>): Map<String, Bitmap> =
        templates.mapNotNull { template ->
            val bitmap = templatePreviews[template.id]
                ?: safely { pdfGenerator.renderTemplatePreview(template.layout) }
                    ?.also { templatePreviews[template.id] = it }
            bitmap?.let { template.id to it }
        }.toMap()

    /** Todas las hojas del currículum, como en el PDF, para "Ver currículum". */
    suspend fun pagesFor(cv: Cv): List<Bitmap> = pdfGenerator.renderPages(cv, layoutFor(cv.templateId))

    private suspend fun preview(cv: Cv): Bitmap? =
        cvPreviews.get(cv) ?: safely { pdfGenerator.renderPreview(cv, layoutFor(cv.templateId)) }
            ?.also { cvPreviews.put(cv, it) }

    /**
     * Diseño de la plantilla del CV. Solo se guarda si el servicio respondió: si falló (por
     * ejemplo, sin internet), la próxima vez se vuelve a intentar en vez de quedarse con el
     * diseño de siempre.
     */
    private suspend fun layoutFor(templateId: String): CvLayout {
        layouts[templateId]?.let { return it }
        val layout = safely { templateRepository.getTemplate(templateId)?.layout ?: CvLayout.DEFAULT }
        return layout?.also { layouts[templateId] = it } ?: CvLayout.DEFAULT
    }

    /** Una vista previa que falla no es un error para el usuario: queda el dibujo de antes. */
    private suspend fun <T> safely(block: suspend () -> T): T? =
        try {
            block()
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            null
        }

    private companion object {
        /** Unos 1,5 MB por imagen: con 12 alcanza para varias pantallas sin gastar de más. */
        const val MAX_CACHED_CVS = 12
    }
}
