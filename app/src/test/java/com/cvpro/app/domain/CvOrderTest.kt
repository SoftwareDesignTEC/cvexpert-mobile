package com.cvpro.app.domain

import com.cvpro.app.domain.model.Cv
import com.cvpro.app.domain.model.UserProfile
import com.cvpro.app.domain.model.byLastEdited
import org.junit.Assert.assertEquals
import org.junit.Test

/** Orden de los currículums por última edición (domain/model/CvOrder.kt). */
class CvOrderTest {

    @Test
    fun `el último editado va primero`() {
        val cvs = listOf(cv("viejo", 1_000L), cv("nuevo", 3_000L), cv("medio", 2_000L))
        assertEquals(listOf("nuevo", "medio", "viejo"), cvs.byLastEdited().map { it.id })
    }

    @Test
    fun `los que no traen fecha van al final`() {
        val cvs = listOf(cv("sin fecha", null), cv("con fecha", 1_000L))
        assertEquals(listOf("con fecha", "sin fecha"), cvs.byLastEdited().map { it.id })
    }

    private fun cv(id: String, updatedAtMillis: Long?) = Cv(
        id = id,
        userId = "u",
        title = id,
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
        workExperiences = emptyList(),
        education = emptyList(),
        skills = emptyList(),
        languages = emptyList(),
        certifications = emptyList(),
        updatedAtMillis = updatedAtMillis,
    )
}
