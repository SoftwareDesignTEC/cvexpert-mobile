package com.cvpro.app.ui.components

import com.cvpro.app.R
import com.cvpro.app.data.mapper.toDomain
import com.cvpro.app.data.mapper.toUpdateRequest
import com.cvpro.app.data.remote.dto.CvDto
import com.cvpro.app.data.remote.dto.EducationDto
import com.cvpro.app.data.remote.dto.LanguageDto
import com.cvpro.app.data.remote.dto.SkillDto
import com.cvpro.app.data.remote.dto.UserProfileDto
import com.cvpro.app.data.remote.dto.WorkExperienceDto
import com.cvpro.app.domain.model.Skill
import com.cvpro.app.domain.model.jobSkillLevels
import org.junit.Assert.*
import org.junit.Test

class CvSkillEditorTest {
    @Test fun localizedLabelOptionsSelectOnlyCanonicalContractValues() {
        val expected = mapOf(
            R.string.cv_skill_level_beginner to "beginner",
            R.string.cv_skill_level_intermediate to "intermediate",
            R.string.cv_skill_level_advanced to "advanced",
            R.string.cv_skill_level_expert to "expert",
        )
        assertEquals(expected, CvSkillLevelOption.entries.associate { it.label to it.canonicalValue })
        assertEquals(jobSkillLevels, expected.values.toSet())
        for (option in CvSkillLevelOption.entries) {
            assertEquals(option, CvSkillLevelOption.fromValue(option.canonicalValue))
            assertNull(FormValidation.skillLevel(option.canonicalValue))
        }
    }

    @Test fun newSkillHasNoSelectedLevelAndCannotBeSavedUntilCompleted() {
        val incomplete = Skill("", "", "", "")
        assertNull(CvSkillLevelOption.fromValue(incomplete.level))
        assertEquals(R.string.cv_skill_level_required, FormValidation.skillLevel(incomplete.level))
        assertTrue(FormValidation.hasInvalidSkills(listOf(incomplete)))
        assertTrue(FormValidation.hasInvalidSkills(listOf(incomplete.copy(name = "Personalizada"))))
        assertFalse(FormValidation.hasInvalidSkills(listOf(incomplete.copy(name = "Personalizada", level = "beginner"))))
    }

    @Test fun previousInvalidLevelsArePreservedAndRequireAnExplicitCorrection() {
        for (raw in listOf("Avanzado", "Principiante", "Experto", "Advanced", "advanced ", "unknown", "B2")) {
            val skill = Skill("skill-legacy", "Habilidad personalizada", raw, "categoría original")
            assertNull(CvSkillLevelOption.fromValue(skill.level))
            assertEquals(R.string.cv_skill_level_invalid, FormValidation.skillLevel(skill.level))
            assertTrue(FormValidation.hasInvalidSkills(listOf(skill)))
            assertEquals(raw, skill.level)
        }
    }

    @Test fun blankNamesAreRejectedAndCustomSkillNamesRemainEditable() {
        for (name in listOf("", "  ", "\t")) {
            assertTrue(FormValidation.hasInvalidSkills(listOf(Skill("", name, "expert", ""))))
        }
        assertFalse(FormValidation.hasInvalidSkills(listOf(Skill("", "Técnica propia del equipo", "expert", ""))))
        assertEquals(R.string.cv_skill_level_required, FormValidation.skillLevel("  "))
    }

    @Test fun removingIncompleteEntriesAllowsSavingIncludingAnEmptySkillList() {
        val valid = Skill("skill-1", "Python", "advanced", "technical")
        val draft = listOf(valid, Skill("", "", "", ""))
        assertTrue(FormValidation.hasInvalidSkills(draft))
        assertFalse(FormValidation.hasInvalidSkills(draft.dropLast(1)))
        assertFalse(FormValidation.hasInvalidSkills(emptyList()))
        assertEquals("skill-1", draft.first().id)
    }

    @Test fun correctingLegacyLevelPreservesAllOtherCvFieldsThroughTheUpdateMapper() {
        val original = existingCv().toDomain()
        val before = original.toUpdateRequest()
        val legacy = original.skills.first()
        for (option in CvSkillLevelOption.entries) {
            // Es la misma copia acotada que recibe el callback del selector del editor.
            val corrected = original.copy(skills = original.skills.mapIndexed { index, skill ->
                if (index == 0) skill.copy(level = option.canonicalValue) else skill
            })
            assertFalse(FormValidation.hasInvalidSkills(corrected.skills))
            assertEquals(legacy.copy(level = option.canonicalValue), corrected.skills.first())
            assertEquals(original, corrected.copy(skills = original.skills))
            val request = corrected.toUpdateRequest()
            val expectedSkills = before.skills!!.mapIndexed { index, skill ->
                if (index == 0) skill.copy(level = option.canonicalValue) else skill
            }
            assertEquals(before.copy(skills = expectedSkills), request)
            assertEquals("skill-legacy", request.skills!!.first().id)
            assertEquals("B2", request.languages!!.single().level)
            assertEquals("Avanzado", original.skills.first().level)
        }
    }

    @Test fun mapperKeepsUnknownOrMissingLevelsUnselectedInsteadOfDefaulting() {
        val dto = existingCv().copy(skills = listOf(
            SkillDto("missing", "Git", null, "technical"),
            SkillDto("unknown", "Linux", "anterior", "custom"),
        ))
        val draft = dto.toDomain()
        assertEquals(listOf("", "anterior"), draft.skills.map { it.level })
        assertTrue(draft.skills.all { CvSkillLevelOption.fromValue(it.level) == null })
        assertTrue(FormValidation.hasInvalidSkills(draft.skills))
        assertEquals(listOf("", "anterior"), draft.toUpdateRequest().skills!!.map { it.level })
    }

    private fun existingCv() = CvDto(
        id = "cv-fixture", userId = "user-fixture", title = "CV de prueba",
        templateId = "template-fixture", previewUrl = "https://example.org/preview",
        createdAt = com.google.gson.JsonPrimitive("2026-01-01T00:00:00Z"), updatedAt = com.google.gson.JsonPrimitive("2026-02-01T00:00:00Z"),
        personalInfo = UserProfileDto(id = "user-fixture", fullName = "Persona de prueba",
            email = "fixture@example.org", address = "Lugar de prueba", bio = "Resumen de prueba",
            types = listOf("full-time"), modalities = listOf("remote"), sectors = listOf("technology")),
        workExperiences = listOf(WorkExperienceDto("work-fixture", "Empresa de prueba", "Puesto",
            com.google.gson.JsonPrimitive("2025-01-01"), null, true, "Descripción", "full-time")),
        education = listOf(EducationDto("education-fixture", "Centro de prueba", "Grado",
            "Área", com.google.gson.JsonPrimitive("2020-01-01"), com.google.gson.JsonPrimitive("2024-01-01"), false)),
        skills = listOf(SkillDto("skill-legacy", "Python", "Avanzado", "technical"),
            SkillDto("skill-other", "Técnica personalizada", "beginner", "custom")),
        languages = listOf(LanguageDto("language-fixture", "English", "B2")),
    )
}
