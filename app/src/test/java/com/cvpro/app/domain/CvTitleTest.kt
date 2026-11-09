package com.cvpro.app.domain

import com.cvpro.app.domain.model.Cv
import com.cvpro.app.domain.model.UserProfile
import com.cvpro.app.domain.model.WorkExperience
import com.cvpro.app.domain.model.autoTitle
import com.cvpro.app.domain.model.titleToSave
import org.junit.Assert.assertEquals
import org.junit.Test

/** Reglas del título automático de los currículums (domain/model/CvTitle.kt). */
class CvTitleTest {

    @Test
    fun `sin experiencias usa el nombre de la plantilla`() {
        assertEquals("Ana Mora – Executive", cv().autoTitle("Executive"))
    }

    @Test
    fun `usa el puesto actual antes que uno más reciente pero terminado`() {
        val cv = cv(
            experiences = listOf(
                job("Cajera", start = 2024, current = true),
                job("Mesera", start = 2025, current = false),
            ),
        )
        assertEquals("Ana Mora – Cajera", cv.autoTitle("Executive"))
    }

    @Test
    fun `al guardar recalcula un título que era el nombre de la plantilla`() {
        val original = cv(title = "Executive")
        val edited = original.copy(workExperiences = listOf(job("Cajera", start = 2024, current = true)))
        assertEquals("Ana Mora – Cajera", edited.titleToSave(original, "Executive"))
    }

    @Test
    fun `al guardar respeta el título que escribió el usuario`() {
        val original = cv(title = "Executive")
        val edited = original.copy(title = "Mi CV para ventas")
        assertEquals("Mi CV para ventas", edited.titleToSave(original, "Executive"))
    }

    @Test
    fun `no toca un título propio que ya estaba guardado`() {
        val original = cv(title = "Mi CV para ventas")
        val edited = original.copy(workExperiences = listOf(job("Cajera", start = 2024, current = true)))
        assertEquals("Mi CV para ventas", edited.titleToSave(original, "Executive"))
    }

    private fun cv(title: String = "", experiences: List<WorkExperience> = emptyList()) = Cv(
        id = "1",
        userId = "u",
        title = title,
        templateId = "executive",
        createdAt = "",
        updatedAt = "",
        previewUrl = null,
        personalInfo = UserProfile(
            id = "u",
            name = "Ana Mora",
            email = "ana@correo.com",
            phone = null,
            location = "",
            photoUrl = null,
            bio = null,
            types = emptyList(),
            modalities = emptyList(),
            sectors = emptyList(),
        ),
        workExperiences = experiences,
        education = emptyList(),
        skills = emptyList(),
        languages = emptyList(),
        certifications = emptyList(),
    )

    private fun job(position: String, start: Int, current: Boolean) = WorkExperience(
        id = position,
        company = "",
        position = position,
        startDate = start * 1000L,
        endDate = null,
        current = current,
        description = "",
        type = "formal",
    )
}
