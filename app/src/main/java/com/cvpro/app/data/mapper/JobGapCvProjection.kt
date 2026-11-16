package com.cvpro.app.data.mapper

import com.cvpro.app.data.remote.dto.JobGapCvDto
import com.cvpro.app.data.remote.dto.JobGapCvSkillDto
import com.cvpro.app.domain.model.Cv
import com.cvpro.app.domain.model.JobSkillsGapException
import com.cvpro.app.domain.model.JobSkillsGapFailure
import com.cvpro.app.domain.model.SkillLevel
import java.text.Normalizer
import java.util.Locale

// ECMAScript trim / \s (Web): Java's default \s omits NBSP/BOM, while Kotlin
// isWhitespace also includes controls that JavaScript does not consider spaces.
private val projectionSpaces = Regex("[\\u0009-\\u000D\\u0020\\u00A0\\u1680\\u2000-\\u200A\\u2028\\u2029\\u202F\\u205F\\u3000\\uFEFF]+")

private fun String.projectionNameKey(): String {
    val lower = replace(projectionSpaces, " ").trim(' ').lowercase(Locale.ROOT)
    val decomposed = Normalizer.normalize(lower, Normalizer.Form.NFD)
    return Normalizer.normalize(decomposed.filterNot {
        it == '\u0300' || it == '\u0301' || it == '\u0302' || it == '\u0308'
    }, Normalizer.Form.NFC)
}

private val languageAliases = listOf(
    "Inglés" to "English", "Español" to "Spanish", "Francés" to "French",
    "Portugués" to "Portuguese", "Alemán" to "German", "Italiano" to "Italian",
    "Ruso" to "Russian", "Mandarín" to "Mandarin", "Japonés" to "Japanese",
    "Coreano" to "Korean", "Árabe" to "Arabic",
    "Lengua de señas costarricense" to "Costa Rican Sign Language",
).flatMap { (spanish, english) ->
    listOf(spanish.projectionNameKey() to spanish.projectionNameKey(),
        english.projectionNameKey() to spanish.projectionNameKey())
}.toMap()

private fun String.competenceIdentity(): String = projectionNameKey().let { languageAliases[it] ?: it }

// Native occupies the fourth position ONLY to adapt cv-skill-v1. It does not
// assert universally certified expert linguistic proficiency or a CEFR level.
private val languageProjectionLevels = mapOf(
    "basic" to "beginner", "intermediate" to "intermediate", "advanced" to "advanced", "native" to "expert",
)

private data class ProjectionSource(val name: String, val level: String, val language: Boolean)

/**
 * Ephemeral request projection, functionally equivalent to Web's projectCvForJob.
 * skills wins over languages; the first equivalent language wins, never the highest level.
 * Does not mutate Cv, persist skills, parse job text or calculate backend results.
 */
internal fun Cv.toJobGapCvProjection(requiredSkills: List<String>): JobGapCvDto {
    val sources = linkedMapOf<String, ProjectionSource>()
    for (skill in skills) {
        if (SkillLevel.fromApi(skill.level) == null)
            throw JobSkillsGapException(JobSkillsGapFailure.INVALID_CV_LEVEL)
        val nameKey = skill.name.projectionNameKey()
        if (nameKey.isEmpty()) throw JobSkillsGapException(JobSkillsGapFailure.INVALID_CV)
        val key = skill.name.competenceIdentity()
        val existing = sources[key]
        val language = nameKey in languageAliases
        if (existing != null && !language && existing.level != skill.level)
            throw JobSkillsGapException(JobSkillsGapFailure.INVALID_CV)
        if (existing == null) sources[key] = ProjectionSource(skill.name, skill.level, language)
    }
    for (language in languages) {
        if (language.name.projectionNameKey().isEmpty())
            throw JobSkillsGapException(JobSkillsGapFailure.INVALID_CV)
        val projectedLevel = languageProjectionLevels[language.level]
            ?: throw JobSkillsGapException(JobSkillsGapFailure.INVALID_CV_LEVEL)
        val key = language.name.competenceIdentity()
        if (key !in sources && requiredSkills.any { it.competenceIdentity() == key })
            sources[key] = ProjectionSource(language.name, projectedLevel, language = true)
    }
    val projected = linkedMapOf<String, JobGapCvSkillDto>()
    for ((key, source) in sources) {
        val names = if (source.language) requiredSkills.filter { it.competenceIdentity() == key } else emptyList()
        // Distinct offer aliases are retained; repeated normalized names are sent only once.
        for (name in names.ifEmpty { listOf(source.name) })
            projected.putIfAbsent(name.projectionNameKey(), JobGapCvSkillDto(name, source.level))
    }
    if (projected.size > 500 || projected.values.any { !it.name.validJobGapText(200, multiline = true) })
        throw JobSkillsGapException(JobSkillsGapFailure.INVALID_CV)
    return JobGapCvDto(id, projected.values.toList())
}
