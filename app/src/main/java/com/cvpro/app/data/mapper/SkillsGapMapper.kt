package com.cvpro.app.data.mapper

import com.cvpro.app.data.remote.dto.CourseDto
import com.cvpro.app.data.remote.dto.SkillInputDto
import com.cvpro.app.data.remote.dto.SkillsGapInputDto
import com.cvpro.app.data.remote.dto.SkillsGapPageDto
import com.cvpro.app.domain.model.CourseRecommendation
import com.cvpro.app.domain.model.SkillGapItem
import com.cvpro.app.domain.model.SkillInput
import com.cvpro.app.domain.model.SkillsGapAnalysis
import com.cvpro.app.domain.model.SkillsGapException
import com.cvpro.app.domain.model.SkillsGapFailure
import com.cvpro.app.domain.model.UnresolvedSkill

fun List<SkillInput>.toInputDto() = SkillsGapInputDto(
    skills = map { SkillInputDto(it.skillName, it.level?.apiValue) },
)

fun SkillsGapPageDto.toDomain(): SkillsGapAnalysis {
    val metadata = analysis.required()
    val status = metadata.status.required()
    val marketStatus = metadata.marketStatus.required()
    val comparedSkills = metadata.comparedSkills.required()
    val scaleKey = metadata.scaleKey.required()
    val page = page.required()
    val pageSize = pageSize.required()
    val totalRecords = totalRecords.required()
    val items = items.required()

    compatible(scaleKey == "cv-skill-v1" && status in setOf("empty", "incomplete", "complete"))
    compatible(marketStatus in setOf("missing", "available") && comparedSkills >= 0)
    compatible(page in 1..9_007_199_254_740_991L && pageSize in 1..100)
    compatible(items.size <= pageSize && items.size <= totalRecords)

    return SkillsGapAnalysis(
        skills = items.map { item ->
            val userLevel = item.userLevel.required()
            val marketLevel = item.marketLevel.required()
            val deficitSteps = item.deficitSteps.required()
            compatible(userLevel in 1..4 && marketLevel in 1..4 && deficitSteps in 1..3)
            compatible(deficitSteps == marketLevel - userLevel)
            SkillGapItem(item.skillName.required(), userLevel, marketLevel, deficitSteps)
        },
        recommendations = items.flatMap { it.courses.required() }.map { it.toDomain() }.distinctBy { it.id },
        status = status,
        comparedSkills = comparedSkills,
        scaleKey = scaleKey,
        marketStatus = marketStatus,
        unresolved = metadata.unresolved.required().map {
            UnresolvedSkill(it.skillName.required(), it.reason.required(), it.userLevel, it.marketLevel)
        },
        page = page,
        pageSize = pageSize,
        totalRecords = totalRecords,
    )
}

private fun CourseDto.toDomain(): CourseRecommendation {
    compatible(durationHours == null || durationHours >= 0)
    return CourseRecommendation(id.required(), title.required(), platform.required(), url.required(), logoUrl, durationHours)
}

private fun compatible(condition: Boolean) {
    if (!condition) throw SkillsGapException(SkillsGapFailure.INCOMPATIBLE_RESPONSE)
}

private fun <T : Any> T?.required(): T =
    this ?: throw SkillsGapException(SkillsGapFailure.INCOMPATIBLE_RESPONSE)
