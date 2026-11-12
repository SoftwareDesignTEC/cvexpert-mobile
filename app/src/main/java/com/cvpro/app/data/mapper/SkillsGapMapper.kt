package com.cvpro.app.data.mapper

import com.cvpro.app.data.remote.dto.SkillsGapPageDto
import com.cvpro.app.domain.model.*
import com.google.gson.JsonArray
import com.google.gson.JsonNull
import com.google.gson.JsonObject

fun SkillsGapPageDto.toDomain(): SkillsGapAnalysis {
    fun compatible(value: Boolean) {
        if (!value) throw SkillsGapException(SkillsGapFailure.INCOMPATIBLE_RESPONSE)
    }
    val metadata = analysis ?: throw SkillsGapException(SkillsGapFailure.INCOMPATIBLE_RESPONSE)
    compatible(metadata.scaleKey == "cv-skill-v1")
    compatible(metadata.status in listOf("complete", "incomplete", "empty"))
    compatible(metadata.marketStatus in listOf("available", "missing"))
    compatible(metadata.comparedSkills != null && metadata.comparedSkills >= 0)
    compatible(metadata.unresolved != null && items != null)
    compatible(page != null && page >= 1 && pageSize != null && pageSize in 1..100 &&
        totalRecords != null && totalRecords >= 0)
    val gaps = items!!.map {
        compatible(it.userLevel in 1..4 && it.marketLevel in 1..4 && it.deficitSteps in 1..3)
        compatible(it.deficitSteps == it.marketLevel!! - it.userLevel!! && it.courses != null)
        SkillGapItem(it.skillName, it.userLevel, it.marketLevel, it.deficitSteps!!)
    }
    compatible(items.size <= pageSize!! && items.size <= totalRecords!!)
    return SkillsGapAnalysis(
        skills = gaps,
        recommendations = items.flatMap { it.courses!! }.distinctBy { it.id }.map {
            compatible(it.durationHours == null || it.durationHours >= 0)
            CourseRecommendation(it.id, it.title, it.platform, it.url, it.logoUrl,
                it.durationHours?.let { hours -> "$hours horas" })
        },
        status = metadata.status!!, comparedSkills = metadata.comparedSkills!!,
        scaleKey = metadata.scaleKey!!, marketStatus = metadata.marketStatus!!,
        unresolved = metadata.unresolved!!.map {
            UnresolvedSkill(it.skillName, it.reason, it.userLevel, it.marketLevel)
        },
        page = page!!, pageSize = pageSize, totalRecords = totalRecords!!,
    )
}

/** Preserve exact names. Unrecognized/missing CV levels mean unknown, never zero. */
fun List<Skill>.toSkillsGapInput(): List<SkillInput> = map {
    SkillInput(it.name, it.level.takeIf { level ->
        level in setOf("beginner", "intermediate", "advanced", "expert")
    })
}

/** Build an explicit JSON tree; the repository sends its exact JSON, retaining nulls. */
fun List<SkillInput>.toInputJson(): JsonObject {
    if (size > 500 || map { it.skillName }.distinct().size != size || any {
        it.skillName.isBlank() || it.skillName.length > 200 ||
            it.skillName != it.skillName.trim() || '\u0000' in it.skillName ||
            (it.level != null && it.level !in setOf("beginner", "intermediate", "advanced", "expert"))
    }) throw SkillsGapException(SkillsGapFailure.INVALID_INPUT)
    return JsonObject().apply {
        add("skills", JsonArray().also { array ->
            forEach { skill -> array.add(JsonObject().apply {
                addProperty("skillName", skill.skillName)
                add("level", skill.level?.let { com.google.gson.JsonPrimitive(it) } ?: JsonNull.INSTANCE)
            }) }
        })
    }
}
