package com.cvpro.app.data.pdf

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import com.cvpro.app.R
import com.cvpro.app.domain.model.Cv
import com.cvpro.app.domain.model.CvLayout
import com.cvpro.app.domain.model.CvLevel
import com.cvpro.app.domain.model.CvLayoutSection
import com.cvpro.app.domain.model.CvSection
import com.cvpro.app.domain.model.Education
import com.cvpro.app.domain.model.HeaderField
import com.cvpro.app.domain.model.Language
import com.cvpro.app.domain.model.LanguageLevel
import com.cvpro.app.domain.model.Skill
import com.cvpro.app.domain.model.SkillLevel
import com.cvpro.app.domain.model.UserProfile
import com.cvpro.app.domain.model.WorkExperience
import com.cvpro.app.domain.model.toCvMonth
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * Arma el PDF de un currículum y lo guarda en el teléfono.
 *
 * Usa PdfDocument, que viene con Android: no hace falta ninguna librería. La página se
 * dibuja a mano sobre un lienzo, igual que el gráfico de radar.
 *
 * Lo que cambia entre plantillas (colores, columnas, secciones y qué campos lleva cada una)
 * llega en un [CvLayout]; acá solo se dibuja. El CvLayout sale de la plantilla del servicio
 * de CVs o, si la plantilla todavía no tiene definición, de [CvLayout.DEFAULT].
 *
 * Los títulos de sección salen de strings.xml, así que el PDF sale en el idioma de la app.
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
        val writer = PageWriter(Paints(layout))
        writer.drawCv(cv, layout)

        val document = PdfDocument()
        writer.render(document)
        val fileName = "${cv.title.replace(Regex("[^A-Za-z0-9 ]"), "").trim()}.pdf"
        val uri = save(document, fileName.ifBlank { "curriculum.pdf" })
        document.close()
        uri
    }

    /**
     * Dibuja la primera hoja del currículum como imagen, para las vistas previas.
     *
     * Es exactamente lo mismo que va en el PDF (se usa el mismo [drawCv]), solo que pintado
     * en un Bitmap y a menor tamaño. Así la vista previa nunca se aleja de lo que el usuario
     * va a descargar. Corre en Dispatchers.Default porque es trabajo de CPU, no de disco.
     */
    suspend fun renderPreview(
        cv: Cv,
        layout: CvLayout = CvLayout.DEFAULT,
        widthPx: Int = PREVIEW_WIDTH_PX,
    ): Bitmap = withContext(Dispatchers.Default) {
        val writer = PageWriter(Paints(layout))
        writer.drawCv(cv, layout)

        // La hoja mide 595 x 842 puntos; se escala para que entre en widthPx.
        val scale = widthPx / PAGE_WIDTH.toFloat()
        // RGB_565 gasta la mitad que ARGB_8888: la hoja es blanca y opaca, no hace falta
        // transparencia, y así se puede dibujar más grande (más nítida) con la misma memoria.
        val bitmap = createBitmap(widthPx, (PAGE_HEIGHT * scale).roundToInt(), Bitmap.Config.RGB_565)
        val canvas = Canvas(bitmap)
        // Fondo blanco como el papel: es la hoja impresa, no un elemento de la interfaz, así
        // que no sigue el tema oscuro.
        canvas.drawColor(Color.WHITE)
        canvas.scale(scale, scale)
        writer.renderFirstPage(canvas)
        bitmap
    }

    /**
     * Todas las hojas del currículum como imágenes, tal como salen en el PDF descargado.
     *
     * Es lo que muestra "Ver currículum": usa el mismo [drawCv] que [generate], así que lo que
     * se ve en pantalla es exactamente el documento (formato, columnas, colores y saltos de
     * hoja), solo que sin tener que abrir otra app.
     */
    suspend fun renderPages(
        cv: Cv,
        layout: CvLayout = CvLayout.DEFAULT,
        widthPx: Int = FULL_PAGE_WIDTH_PX,
    ): List<Bitmap> = withContext(Dispatchers.Default) {
        val writer = PageWriter(Paints(layout))
        writer.drawCv(cv, layout)
        val scale = widthPx / PAGE_WIDTH.toFloat()
        (0 until writer.pageCount).map { index ->
            val bitmap = createBitmap(widthPx, (PAGE_HEIGHT * scale).roundToInt(), Bitmap.Config.RGB_565)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            canvas.scale(scale, scale)
            writer.renderPage(index, canvas)
            bitmap
        }
    }

    /**
     * Vista previa de una plantilla, con un currículum de ejemplo.
     *
     * El servicio de CVs no manda una imagen de cada plantilla (su previewUrl es un rectángulo
     * gris con el nombre), así que se dibuja acá con el mismo motor del PDF.
     */
    suspend fun renderTemplatePreview(layout: CvLayout?): Bitmap =
        renderPreview(sampleCv(), layout ?: CvLayout.DEFAULT)

    /** Currículum inventado para las vistas previas. Los textos salen de strings.xml. */
    private fun sampleCv(): Cv {
        val name = context.getString(R.string.sample_cv_name)
        return Cv(
            id = "sample",
            userId = "",
            title = context.getString(R.string.sample_cv_job1_position),
            templateId = "",
            createdAt = "",
            updatedAt = "",
            previewUrl = null,
            personalInfo = UserProfile(
                id = "",
                name = name,
                email = context.getString(R.string.sample_cv_email),
                phone = context.getString(R.string.sample_cv_phone),
                location = context.getString(R.string.sample_cv_location),
                photoUrl = null,
                bio = context.getString(R.string.sample_cv_bio),
                types = emptyList(),
                modalities = emptyList(),
                sectors = emptyList(),
            ),
            workExperiences = listOf(
                WorkExperience(
                    id = "1",
                    company = context.getString(R.string.sample_cv_job1_company),
                    position = context.getString(R.string.sample_cv_job1_position),
                    startDate = epoch(2024, 3),
                    endDate = null,
                    current = true,
                    description = context.getString(R.string.sample_cv_job1_description),
                    type = "formal",
                ),
                WorkExperience(
                    id = "2",
                    company = context.getString(R.string.sample_cv_job2_company),
                    position = context.getString(R.string.sample_cv_job2_position),
                    startDate = epoch(2023, 1),
                    endDate = epoch(2023, 12),
                    current = false,
                    description = context.getString(R.string.sample_cv_job2_description),
                    type = "volunteer",
                ),
            ),
            education = listOf(
                Education(
                    id = "1",
                    institution = context.getString(R.string.sample_cv_institution),
                    degree = context.getString(R.string.sample_cv_degree),
                    field = "",
                    startDate = epoch(2019, 2),
                    endDate = epoch(2023, 12),
                    current = false,
                ),
            ),
            skills = listOf(
                Skill("1", context.getString(R.string.sample_cv_skill1), SkillLevel.ADVANCED.apiValue, ""),
                Skill("2", context.getString(R.string.sample_cv_skill2), SkillLevel.INTERMEDIATE.apiValue, ""),
                Skill("3", context.getString(R.string.sample_cv_skill3), SkillLevel.ADVANCED.apiValue, ""),
            ),
            languages = listOf(
                Language("1", context.getString(R.string.sample_cv_language1), LanguageLevel.NATIVE.apiValue),
                Language("2", context.getString(R.string.sample_cv_language2), LanguageLevel.INTERMEDIATE.apiValue),
            ),
            certifications = emptyList(),
        )
    }

    /** Primer día del mes, en epoch milisegundos UTC, como las fechas del servicio de CVs. */
    private fun epoch(year: Int, month: Int): Long =
        LocalDate.of(year, month, 1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    /**
     * Encabezado arriba, a todo el ancho, y después las secciones en el orden de la plantilla.
     * Con 2 columnas, las dos arrancan a la misma altura, justo debajo del encabezado.
     */
    private fun PageWriter.drawCv(cv: Cv, layout: CvLayout) {
        drawHeader(cv, layout)

        if (layout.columns == 2) {
            val start = position()
            column(left = MARGIN, columnWidth = MAIN_COLUMN_WIDTH, from = start)
            layout.sections.filter { it.column == 1 }.forEach { drawSection(cv, it) }
            column(left = MARGIN + MAIN_COLUMN_WIDTH + COLUMN_GAP, columnWidth = SIDE_COLUMN_WIDTH, from = start)
            layout.sections.filter { it.column == 2 }.forEach { drawSection(cv, it) }
        } else {
            layout.sections.forEach { drawSection(cv, it) }
        }
    }

    /**
     * Nombre, contacto y resumen, según los campos que pida la plantilla. El orden de los
     * bloques es fijo (nombre, contacto, resumen); dentro del contacto se respeta el de la
     * plantilla.
     */
    private fun PageWriter.drawHeader(cv: Cv, layout: CvLayout) {
        val fields = layout.header
        val profile = cv.personalInfo

        if (HeaderField.FULL_NAME in fields) text(profile.name, paints.name)
        if (layout.showCvTitle) text(cv.title, paints.title)

        val contacto = fields.mapNotNull { field ->
            when (field) {
                HeaderField.EMAIL -> profile.email
                HeaderField.PHONE -> profile.phone
                HeaderField.ADDRESS -> profile.location
                else -> null
            }
        }.filter { it.isNotBlank() }.joinToString("  ·  ")
        text(contacto, paints.small, gapBefore = 2f)

        if (HeaderField.BIO in fields) {
            profile.bio?.takeIf { it.isNotBlank() }?.let { text(it, paints.body, gapBefore = 8f) }
        }
    }

    /** Una sección con solo los campos que pide la plantilla, en su orden. */
    private fun PageWriter.drawSection(cv: Cv, section: CvLayoutSection) {
        val fields = section.fields
        when (section.source) {
            CvSection.EXPERIENCE -> drawEntries(
                title = R.string.cv_detail_experience,
                entries = cv.workExperiences.map { exp ->
                    entryOf(
                        fields = fields,
                        values = mapOf(
                            "position" to exp.position,
                            "company" to exp.company,
                            "description" to exp.description,
                            "type" to experienceType(exp.type),
                        ),
                        period = period(fields, "startDate", "endDate", exp.startDate, exp.endDate, exp.current),
                    )
                },
            )

            CvSection.EDUCATION -> drawEntries(
                title = R.string.cv_detail_education,
                entries = cv.education.map { edu ->
                    entryOf(
                        fields = fields,
                        values = mapOf(
                            "degree" to edu.degree,
                            "institution" to edu.institution,
                            "field" to edu.field,
                        ),
                        period = period(fields, "startDate", "endDate", edu.startDate, edu.endDate, edu.current),
                    )
                },
            )

            CvSection.SKILLS -> drawEntries(
                title = R.string.cv_detail_skills,
                compact = true,
                entries = cv.skills.map { skill ->
                    entryOf(
                        fields = fields,
                        values = mapOf(
                            "name" to skill.name,
                            "level" to levelLabel(SkillLevel.fromApi(skill.level), skill.level),
                            "category" to skill.category,
                        ),
                    )
                },
            )

            CvSection.LANGUAGES -> drawEntries(
                title = R.string.cv_detail_languages,
                compact = true,
                entries = cv.languages.map { language ->
                    entryOf(fields = fields, values = mapOf("name" to language.name, "level" to levelLabel(LanguageLevel.fromApi(language.level), language.level)))
                },
            )

            CvSection.CERTIFICATIONS -> drawEntries(
                title = R.string.cv_detail_certifications,
                entries = cv.certifications.map { cert ->
                    entryOf(
                        fields = fields,
                        values = mapOf(
                            "name" to cert.name,
                            "issuer" to cert.issuer,
                            "credentialUrl" to cert.credentialUrl.orEmpty(),
                        ),
                        period = period(fields, "issuedAt", "expiresAt", cert.issuedAt, cert.expiresAt, current = false),
                    )
                },
            )
        }
    }

    /**
     * Dibuja los elementos de una sección. Si ninguno tiene nada que mostrar, la sección entera
     * se omite (ni siquiera el título).
     *
     * @param compact habilidades e idiomas van todos en un párrafo: "Kotlin (advanced)  ·  Git".
     */
    private fun PageWriter.drawEntries(title: Int, entries: List<Entry>, compact: Boolean = false) {
        val visibles = entries.filterNot { it.isEmpty }
        if (visibles.isEmpty()) return
        section(sectionTitle(title))

        if (compact) {
            text(visibles.joinToString("  ·  ") { it.inline() }, paints.body)
            return
        }
        visibles.forEach { entry ->
            keepTogether(paints.heading.textSize + paints.small.textSize + LINE_GAP * 2)
            text(entry.heading.joinToString(" · "), paints.heading)
            text(entry.details.joinToString("   "), paints.small)
            entry.body.forEach { text(it, paints.body) }
            space(ENTRY_GAP)
        }
    }

    /**
     * Reparte los campos de un elemento en tres renglones: el título en negrita (puesto,
     * empresa, nombre…), los detalles en letra chica (periodo, área, nivel…) y el texto largo
     * (descripción). Dentro de cada renglón queda el orden de la plantilla.
     *
     * @param values el texto de cada campo, ya listo para mostrar.
     * @param period el periodo ya armado; ocupa el lugar del primer campo de fecha.
     */
    private fun entryOf(fields: List<String>, values: Map<String, String>, period: String = ""): Entry {
        val entry = Entry()
        var periodDone = false
        fields.forEach { field ->
            when (field) {
                in HEADING_FIELDS -> entry.heading += values[field].orEmpty()
                in DATE_FIELDS -> if (!periodDone) {
                    entry.details += period
                    periodDone = true
                }
                "description" -> entry.body += values[field].orEmpty()
                else -> entry.details += values[field].orEmpty()
            }
        }
        entry.heading.removeAll { it.isBlank() }
        entry.details.removeAll { it.isBlank() }
        entry.body.removeAll { it.isBlank() }
        return entry
    }

    /** Lo que se escribe de un elemento de una sección. */
    private class Entry {
        val heading = mutableListOf<String>()
        val details = mutableListOf<String>()
        val body = mutableListOf<String>()

        val isEmpty get() = heading.isEmpty() && details.isEmpty() && body.isEmpty()

        /** "Kotlin (advanced, Programación)", para las secciones que van en un solo párrafo. */
        fun inline(): String {
            val nombre = heading.joinToString(" · ")
            val extra = details.joinToString(", ")
            return when {
                nombre.isEmpty() -> extra
                extra.isEmpty() -> nombre
                else -> "$nombre ($extra)"
            }
        }
    }

    /**
     * "06/2024 — 03/2025", "06/2024 — Actual" o solo una de las dos fechas, según qué campos
     * de fecha pida la plantilla. Si el periodo sigue vigente se escribe "Actual" aunque la
     * plantilla pida endDate, porque en ese caso el servicio la manda en null.
     */
    private fun period(
        fields: List<String>,
        startField: String,
        endField: String,
        start: Long?,
        end: Long?,
        current: Boolean,
    ): String {
        val desde = if (startField in fields) start.toCvMonth() else ""
        val hasta = when {
            current && (endField in fields || "current" in fields) -> context.getString(R.string.cv_detail_present)
            endField in fields -> end.toCvMonth()
            else -> ""
        }
        return when {
            desde.isNotEmpty() && hasta.isNotEmpty() -> context.getString(R.string.cv_detail_period, desde, hasta)
            else -> desde + hasta
        }
    }

    /** "volunteer" → "Voluntariado". Un tipo que la app no conoce se escribe tal cual. */
    private fun experienceType(type: String): String = when (type) {
        "formal" -> context.getString(R.string.cv_experience_type_formal)
        "freelance" -> context.getString(R.string.cv_experience_type_freelance)
        "volunteer" -> context.getString(R.string.cv_experience_type_volunteer)
        "school-project" -> context.getString(R.string.cv_experience_type_school_project)
        "informal" -> context.getString(R.string.cv_experience_type_informal)
        else -> type
    }

    /** "advanced" → "Avanzado". Un nivel que la app no conoce se escribe tal cual. */
    private fun levelLabel(level: CvLevel?, raw: String): String =
        level?.let { context.getString(it.labelRes) } ?: raw

    /** "Experiencia laboral" → "EXPERIENCIA LABORAL", en el idioma de la app. */
    private fun sectionTitle(resId: Int): String =
        context.getString(resId).uppercase(context.resources.configuration.locales[0])

    /**
     * Escribe de arriba hacia abajo y abre una hoja nueva cuando no queda espacio.
     *
     * No dibuja directo en el PDF: anota qué va en cada hoja y lo dibuja todo al final, en
     * [render]. Hace falta por las 2 columnas: la de la izquierda puede seguir en la hoja 2 y
     * después la de la derecha tiene que volver a la hoja 1, pero PdfDocument no deja volver a
     * una hoja que ya se cerró.
     */
    private class PageWriter(val paints: Paints) {

        /** Lo que hay que dibujar en cada hoja, en orden. */
        private val pages = mutableListOf<MutableList<(Canvas) -> Unit>>(mutableListOf())
        private var pageIndex = 0

        /** Altura de la próxima línea (su base, que es donde Canvas.drawText apoya el texto). */
        private var y = MARGIN

        /** Borde izquierdo y ancho de donde se está escribiendo: la hoja entera o una columna. */
        private var x = MARGIN
        private var width = CONTENT_WIDTH

        /** Dónde va el próximo renglón, para que las dos columnas arranquen a la misma altura. */
        data class Position(val pageIndex: Int, val y: Float)

        fun position() = Position(pageIndex, y)

        /** Pasa a escribir en una columna, empezando en [from]. */
        fun column(left: Float, columnWidth: Float, from: Position) {
            x = left
            width = columnWidth
            pageIndex = from.pageIndex
            y = from.y
        }

        /** Escribe un texto cortándolo en renglones que quepan en el ancho disponible. */
        fun text(text: String, paint: Paint, gapBefore: Float = 0f) {
            if (text.isBlank()) return
            y += gapBefore
            wrap(text, paint).forEach { drawLine(it, paint) }
        }

        /** Título de sección con su línea divisoria. Nunca queda solo al pie de una hoja. */
        fun section(title: String) {
            keepTogether(SECTION_TOP + SECTION_LINE_GAP + paints.section.textSize + MIN_CONTENT)
            y += SECTION_TOP
            val lineY = y
            val left = x
            val right = x + width
            draw { it.drawLine(left, lineY, right, lineY, paints.divider) }
            y += SECTION_LINE_GAP
            text(title, paints.section)
        }

        /** Si lo que sigue no cabe entero en la hoja, empieza en la siguiente. */
        fun keepTogether(height: Float) {
            if (y + height > PAGE_HEIGHT - MARGIN) nextPage()
        }

        fun space(height: Float) {
            y += height
        }

        /** Dibuja solo la primera hoja en un lienzo cualquiera (para las vistas previas). */
        fun renderFirstPage(canvas: Canvas) {
            pages.first().forEach { it(canvas) }
        }

        /** Cuántas hojas ocupa el currículum. */
        val pageCount: Int get() = pages.size

        /** Dibuja la hoja [index] en un lienzo cualquiera ("Ver currículum"). */
        fun renderPage(index: Int, canvas: Canvas) {
            pages[index].forEach { it(canvas) }
        }

        /** Dibuja todas las hojas en el documento, una tras otra. */
        fun render(document: PdfDocument) {
            pages.forEachIndexed { index, operations ->
                val page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, index + 1).create())
                operations.forEach { it(page.canvas) }
                document.finishPage(page)
            }
        }

        /** Un renglón. Si no cabe en la hoja actual, va al principio de la siguiente. */
        private fun drawLine(text: String, paint: Paint) {
            if (y + paint.textSize > PAGE_HEIGHT - MARGIN) nextPage()
            // Se copian los valores: la lambda corre al final, cuando x e y ya cambiaron.
            val left = x
            val baseline = y
            draw { it.drawText(text, left, baseline, paint) }
            y += paint.textSize + LINE_GAP
        }

        /**
         * Corta el texto en renglones por palabras. Una palabra que sola no cabe (un enlace
         * largo en la columna angosta) se corta por letras para que no se salga de la hoja.
         */
        private fun wrap(text: String, paint: Paint): List<String> {
            val renglones = mutableListOf<String>()
            var actual = ""
            text.split(" ").forEach { palabra ->
                val prueba = if (actual.isEmpty()) palabra else "$actual $palabra"
                if (paint.measureText(prueba) <= width) {
                    actual = prueba
                    return@forEach
                }
                if (actual.isNotEmpty()) renglones += actual
                actual = palabra
                while (paint.measureText(actual) > width) {
                    val caben = paint.breakText(actual, true, width, null).coerceAtLeast(1)
                    renglones += actual.take(caben)
                    actual = actual.drop(caben)
                }
            }
            if (actual.isNotEmpty()) renglones += actual
            return renglones
        }

        private fun draw(operation: (Canvas) -> Unit) {
            pages[pageIndex] += operation
        }

        /** Sigue en la hoja siguiente; si todavía no existe (primera columna), la crea. */
        private fun nextPage() {
            pageIndex++
            if (pageIndex == pages.size) pages += mutableListOf<(Canvas) -> Unit>()
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

        // Ancho de las vistas previas: la tarjeta de Mis Currículums ocupa todo el ancho del
        // teléfono, y con menos se veía borrosa. En RGB_565 son unos 1,5 MB por imagen.
        const val PREVIEW_WIDTH_PX = 720

        /** Ancho de cada hoja en "Ver currículum": más que la pantalla, para que se lea nítida al acercarla. */
        const val FULL_PAGE_WIDTH_PX = 1600

        // Separación de los títulos de sección, igual que antes: 10 hasta la línea y 16 hasta el texto.
        const val SECTION_TOP = 10f
        const val SECTION_LINE_GAP = 16f

        // Lo mínimo que tiene que caber debajo de un título para no dejarlo solo al pie de la hoja.
        const val MIN_CONTENT = 30f

        // Ancho útil de la hoja y de las 2 columnas: la principal (izquierda) se queda con un
        // poco más del 60 % y la lateral con el resto, como en la mayoría de los CV a dos columnas.
        const val CONTENT_WIDTH = PAGE_WIDTH - MARGIN * 2
        const val COLUMN_GAP = 20f
        const val MAIN_COLUMN_WIDTH = (CONTENT_WIDTH - COLUMN_GAP) * 0.62f
        const val SIDE_COLUMN_WIDTH = CONTENT_WIDTH - COLUMN_GAP - MAIN_COLUMN_WIDTH

        // Campos que van en el renglón en negrita o que forman el periodo (ver entryOf).
        val HEADING_FIELDS = setOf("position", "company", "degree", "institution", "name", "issuer")
        val DATE_FIELDS = setOf("startDate", "endDate", "current", "issuedAt", "expiresAt")
    }
}
