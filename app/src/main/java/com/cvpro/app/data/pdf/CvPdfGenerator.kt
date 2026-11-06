package com.cvpro.app.data.pdf

import android.content.ContentValues
import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.cvpro.app.R
import com.cvpro.app.domain.model.Cv
import com.cvpro.app.domain.model.CvLayout
import com.cvpro.app.domain.model.CvSection
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Arma el PDF de un currículum y lo guarda en el teléfono.
 *
 * Usa PdfDocument, que viene con Android: no hace falta ninguna librería. La página se
 * dibuja a mano sobre un lienzo, igual que el gráfico de radar.
 *
 * Lo que cambia entre plantillas (colores, tamaños y orden de secciones) llega en un
 * [CvLayout]; acá solo se dibuja. Así, cuando el servicio de CVs mande plantillas
 * estructuradas, este archivo no cambia: solo se arma otro CvLayout.
 *
 * Los títulos de sección salen de strings.xml, así que el PDF sale en el idioma del teléfono.
 * Si el currículum no cabe en una hoja, sigue en la siguiente.
 *
 * El archivo va a la carpeta de Descargas. Desde Android 10 eso se hace con MediaStore y
 * no requiere permisos; en versiones anteriores se guarda en la carpeta propia de la app,
 * que tampoco los pide, y se comparte con FileProvider.
 */
@Singleton
class CvPdfGenerator @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    /** Genera el PDF y devuelve a dónde quedó, listo para abrirse. */
    suspend fun generate(cv: Cv, layout: CvLayout = CvLayout.DEFAULT): Uri = withContext(Dispatchers.IO) {
        val document = PdfDocument()
        val writer = PageWriter(document, Paints(layout))
        writer.drawCv(cv, layout)
        writer.finish()

        val fileName = "${cv.title.replace(Regex("[^A-Za-z0-9 ]"), "").trim()}.pdf"
        val uri = save(document, fileName.ifBlank { "curriculum.pdf" })
        document.close()
        uri
    }

    /** Encabezado fijo arriba y después las secciones en el orden que diga la plantilla. */
    private fun PageWriter.drawCv(cv: Cv, layout: CvLayout) {
        line(cv.personalInfo.name, paints.name)
        line(cv.title, paints.title)

        val contacto = listOfNotNull(
            cv.personalInfo.email,
            cv.personalInfo.phone,
            cv.personalInfo.location,
        ).joinToString("  ·  ")
        line(contacto, paints.small, gapBefore = 2f)

        cv.personalInfo.bio?.takeIf { it.isNotBlank() }?.let { bio ->
            paragraph(bio, paints.body, gapBefore = 8f)
        }

        layout.sections.forEach { section ->
            when (section) {
                CvSection.EXPERIENCE -> drawExperience(cv)
                CvSection.EDUCATION -> drawEducation(cv)
                CvSection.SKILLS -> drawSkills(cv)
                CvSection.LANGUAGES -> drawLanguages(cv)
            }
        }
    }

    private fun PageWriter.drawExperience(cv: Cv) {
        if (cv.workExperiences.isEmpty()) return
        section(sectionTitle(R.string.cv_detail_experience))
        cv.workExperiences.forEach { exp ->
            keepTogether(paints.heading.textSize + paints.small.textSize + LINE_GAP * 2)
            line("${exp.position} · ${exp.company}", paints.heading)
            line(period(exp.startDate, exp.endDate, exp.current), paints.small)
            if (exp.description.isNotBlank()) paragraph(exp.description, paints.body)
            space(ENTRY_GAP)
        }
    }

    private fun PageWriter.drawEducation(cv: Cv) {
        if (cv.education.isEmpty()) return
        section(sectionTitle(R.string.cv_detail_education))
        cv.education.forEach { edu ->
            keepTogether(paints.heading.textSize + paints.small.textSize + LINE_GAP * 2)
            line("${edu.degree} · ${edu.institution}", paints.heading)
            line("${edu.field}   ${period(edu.startDate, edu.endDate, edu.current)}", paints.small)
            space(ENTRY_GAP)
        }
    }

    private fun PageWriter.drawSkills(cv: Cv) {
        if (cv.skills.isEmpty()) return
        section(sectionTitle(R.string.cv_detail_skills))
        paragraph(cv.skills.joinToString("  ·  ") { "${it.name} (${it.level})" }, paints.body)
    }

    private fun PageWriter.drawLanguages(cv: Cv) {
        if (cv.languages.isEmpty()) return
        section(sectionTitle(R.string.cv_detail_languages))
        paragraph(cv.languages.joinToString("  ·  ") { "${it.name} (${it.level})" }, paints.body)
    }

    /** "Experiencia laboral" → "EXPERIENCIA LABORAL", en el idioma del teléfono. */
    private fun sectionTitle(resId: Int): String =
        context.getString(resId).uppercase(context.resources.configuration.locales[0])

    /** "2024-01 — 2025-06" o "2024-01 — Actual", con los mismos textos que el detalle en pantalla. */
    private fun period(start: String, end: String?, current: Boolean): String =
        if (current) {
            context.getString(R.string.cv_detail_period_current, start)
        } else {
            context.getString(R.string.cv_detail_period, start, end.orEmpty())
        }

    /**
     * Escribe de arriba hacia abajo y abre una hoja nueva cuando no queda espacio.
     *
     * Antes todo se dibujaba en una sola hoja: lo que no cabía se perdía sin aviso.
     */
    private class PageWriter(private val document: PdfDocument, val paints: Paints) {

        private var pageNumber = 0
        private lateinit var page: PdfDocument.Page

        /** Altura de la próxima línea (su base, que es donde Canvas.drawText apoya el texto). */
        private var y = MARGIN

        init {
            newPage()
        }

        fun finish() = document.finishPage(page)

        /** Escribe una línea. Si no cabe en la hoja actual, va al principio de la siguiente. */
        fun line(text: String, paint: Paint, gapBefore: Float = 0f) {
            if (text.isBlank()) return
            y += gapBefore
            if (y + paint.textSize > PAGE_HEIGHT - MARGIN) newPage()
            page.canvas.drawText(text, MARGIN, y, paint)
            y += paint.textSize + LINE_GAP
        }

        /** Escribe un párrafo cortándolo en líneas que quepan en el ancho de la página. */
        fun paragraph(text: String, paint: Paint, gapBefore: Float = 0f) {
            val ancho = PAGE_WIDTH - MARGIN * 2
            y += gapBefore
            var linea = StringBuilder()
            text.split(" ").forEach { palabra ->
                val prueba = if (linea.isEmpty()) palabra else "$linea $palabra"
                if (paint.measureText(prueba) > ancho) {
                    line(linea.toString(), paint)
                    linea = StringBuilder(palabra)
                } else {
                    linea = StringBuilder(prueba)
                }
            }
            line(linea.toString(), paint)
        }

        /** Título de sección con su línea divisoria. Nunca queda solo al pie de una hoja. */
        fun section(title: String) {
            keepTogether(SECTION_TOP + SECTION_LINE_GAP + paints.section.textSize + MIN_CONTENT)
            y += SECTION_TOP
            page.canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, paints.divider)
            y += SECTION_LINE_GAP
            line(title, paints.section)
        }

        /** Si lo que sigue no cabe entero en la hoja, empieza en la siguiente. */
        fun keepTogether(height: Float) {
            if (y + height > PAGE_HEIGHT - MARGIN) newPage()
        }

        fun space(height: Float) {
            y += height
        }

        private fun newPage() {
            if (pageNumber > 0) document.finishPage(page)
            pageNumber++
            page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
            y = MARGIN
        }
    }

    /** Los pinceles de una plantilla, armados una vez por PDF. */
    private class Paints(layout: CvLayout) {
        val name = paint(layout.nameSize, layout.textColor, bold = true)
        val title = paint(layout.titleSize, layout.mutedColor)
        val section = paint(layout.sectionSize, layout.accentColor, bold = true)
        val heading = paint(layout.headingSize, layout.textColor, bold = true)
        val body = paint(layout.bodySize, layout.textColor)
        val small = paint(layout.smallSize, layout.mutedColor)
        val divider = Paint().apply { strokeWidth = 0.8f; color = layout.dividerColor }

        private fun paint(size: Float, color: Int, bold: Boolean = false) = Paint().apply {
            textSize = size
            this.color = color
            if (bold) typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
    }

    /** Guarda el documento y devuelve su ubicación. */
    private fun save(document: PdfDocument, fileName: String): Uri =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10 en adelante: va a Descargas sin pedir permisos.
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, PDF_MIME)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = requireNotNull(
                resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values),
            ) { "No se pudo crear el archivo en Descargas" }

            resolver.openOutputStream(uri)?.use { document.writeTo(it) }
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            uri
        } else {
            // Android 9 y anteriores: carpeta propia de la app, que no pide permisos.
            val carpeta = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            val archivo = File(carpeta, fileName)
            archivo.outputStream().use { document.writeTo(it) }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)
        }

    private companion object {
        // A4 a 72 puntos por pulgada, que es la unidad que usa PdfDocument.
        const val PAGE_WIDTH = 595
        const val PAGE_HEIGHT = 842
        const val MARGIN = 48f
        const val LINE_GAP = 4f
        const val ENTRY_GAP = 6f
        const val PDF_MIME = "application/pdf"

        // Separación de los títulos de sección, igual que antes: 10 hasta la línea y 16 hasta el texto.
        const val SECTION_TOP = 10f
        const val SECTION_LINE_GAP = 16f

        // Lo mínimo que tiene que caber debajo de un título para no dejarlo solo al pie de la hoja.
        const val MIN_CONTENT = 30f
    }
}
