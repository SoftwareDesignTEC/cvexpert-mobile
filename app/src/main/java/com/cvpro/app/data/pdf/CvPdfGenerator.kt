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
import com.cvpro.app.domain.model.Cv
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
 * El archivo va a la carpeta de Descargas. Desde Android 10 eso se hace con MediaStore y
 * no requiere permisos; en versiones anteriores se guarda en la carpeta propia de la app,
 * que tampoco los pide, y se comparte con FileProvider.
 */
@Singleton
class CvPdfGenerator @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    /** Genera el PDF y devuelve a dónde quedó, listo para abrirse. */
    suspend fun generate(cv: Cv): Uri = withContext(Dispatchers.IO) {
        val document = PdfDocument()
        val page = document.startPage(
            PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create(),
        )

        drawCv(page.canvas, cv)
        document.finishPage(page)

        val fileName = "${cv.title.replace(Regex("[^A-Za-z0-9 ]"), "").trim()}.pdf"
        val uri = save(document, fileName.ifBlank { "curriculum.pdf" })
        document.close()
        uri
    }

    /** Dibuja el contenido del currículum sobre la página. */
    private fun drawCv(canvas: android.graphics.Canvas, cv: Cv) {
        var y = MARGIN

        y = canvas.write(cv.personalInfo.name, y, titlePaint)
        y = canvas.write(cv.title, y, subtitlePaint)

        val contacto = listOfNotNull(
            cv.personalInfo.email,
            cv.personalInfo.phone,
            cv.personalInfo.location,
        ).joinToString("  ·  ")
        y = canvas.write(contacto, y + 2f, smallPaint)

        cv.personalInfo.bio?.takeIf { it.isNotBlank() }?.let { bio ->
            y = canvas.wrap(bio, y + 8f, bodyPaint)
        }

        if (cv.workExperiences.isNotEmpty()) {
            y = canvas.section("EXPERIENCIA LABORAL", y)
            cv.workExperiences.forEach { exp ->
                val periodo = if (exp.current) "${exp.startDate} — Actual" else "${exp.startDate} — ${exp.endDate.orEmpty()}"
                y = canvas.write("${exp.position} · ${exp.company}", y, boldPaint)
                y = canvas.write(periodo, y, smallPaint)
                if (exp.description.isNotBlank()) y = canvas.wrap(exp.description, y, bodyPaint)
                y += 6f
            }
        }

        if (cv.education.isNotEmpty()) {
            y = canvas.section("EDUCACIÓN", y)
            cv.education.forEach { edu ->
                val periodo = if (edu.current) "${edu.startDate} — Actual" else "${edu.startDate} — ${edu.endDate.orEmpty()}"
                y = canvas.write("${edu.degree} · ${edu.institution}", y, boldPaint)
                y = canvas.write("${edu.field}   $periodo", y, smallPaint)
                y += 6f
            }
        }

        if (cv.skills.isNotEmpty()) {
            y = canvas.section("HABILIDADES", y)
            y = canvas.wrap(cv.skills.joinToString("  ·  ") { "${it.name} (${it.level})" }, y, bodyPaint)
        }

        if (cv.languages.isNotEmpty()) {
            y = canvas.section("IDIOMAS", y)
            canvas.wrap(cv.languages.joinToString("  ·  ") { "${it.name} (${it.level})" }, y, bodyPaint)
        }
    }

    /** Escribe una línea y devuelve la altura donde sigue la siguiente. */
    private fun android.graphics.Canvas.write(text: String, y: Float, paint: Paint): Float {
        if (text.isBlank()) return y
        drawText(text, MARGIN, y, paint)
        return y + paint.textSize + LINE_GAP
    }

    /** Escribe un párrafo cortándolo en líneas que quepan en el ancho de la página. */
    private fun android.graphics.Canvas.wrap(text: String, y: Float, paint: Paint): Float {
        val ancho = PAGE_WIDTH - MARGIN * 2
        var actual = y
        var linea = StringBuilder()
        text.split(" ").forEach { palabra ->
            val prueba = if (linea.isEmpty()) palabra else "$linea $palabra"
            if (paint.measureText(prueba) > ancho) {
                actual = write(linea.toString(), actual, paint)
                linea = StringBuilder(palabra)
            } else {
                linea = StringBuilder(prueba)
            }
        }
        return write(linea.toString(), actual, paint)
    }

    /** Título de sección con su línea divisoria. */
    private fun android.graphics.Canvas.section(title: String, y: Float): Float {
        val arriba = y + 10f
        drawLine(MARGIN, arriba, PAGE_WIDTH - MARGIN, arriba, linePaint)
        return write(title, arriba + 16f, sectionPaint)
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

    private val titlePaint = Paint().apply {
        textSize = 22f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val subtitlePaint = Paint().apply { textSize = 13f; color = GRIS }
    private val sectionPaint = Paint().apply {
        textSize = 11f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        color = AZUL
    }
    private val boldPaint = Paint().apply {
        textSize = 11f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val bodyPaint = Paint().apply { textSize = 10f }
    private val smallPaint = Paint().apply { textSize = 9f; color = GRIS }
    private val linePaint = Paint().apply { strokeWidth = 0.8f; color = GRIS_CLARO }

    private companion object {
        // A4 a 72 puntos por pulgada, que es la unidad que usa PdfDocument.
        const val PAGE_WIDTH = 595
        const val PAGE_HEIGHT = 842
        const val MARGIN = 48f
        const val LINE_GAP = 4f
        const val PDF_MIME = "application/pdf"

        const val AZUL = 0xFF2563EB.toInt()
        const val GRIS = 0xFF64748B.toInt()
        const val GRIS_CLARO = 0xFFCBD5E1.toInt()
    }
}
