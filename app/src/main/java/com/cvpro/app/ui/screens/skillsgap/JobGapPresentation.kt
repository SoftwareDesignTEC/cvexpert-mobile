package com.cvpro.app.ui.screens.skillsgap

import com.cvpro.app.domain.model.*

/** Display projection only. Comparison and deficit calculations remain exclusively in the backend. */
internal data class SkillComparison(
    val name: String, val cvLevel: SkillLevel?, val requiredLevel: SkillLevel?,
    val assessment: JobLevelAssessment?,
)

internal fun SkillLevel.ordinalPosition(): Int = when (this) {
    SkillLevel.BEGINNER -> 1
    SkillLevel.INTERMEDIATE -> 2
    SkillLevel.ADVANCED -> 3
    SkillLevel.EXPERT -> 4
}

internal data class JobGapPresentation(val analysis: JobSkillsGapAnalysis) {
    private val assessments = analysis.assessments.orEmpty().associateBy { it.name }
    val documented = analysis.matchedSkills.map {
        SkillComparison(it.name, SkillLevel.fromApi(it.cvLevel), assessments[it.name]?.requiredLevel, assessments[it.name])
    }
    val below = documented.filter { it.assessment?.status == "documented_skill_below_required" }
    val meets = documented.filter { it.assessment?.status == "documented_skill_meets_required" }
    val unspecified = documented.filter { it.requiredLevel == null }
    val missing = analysis.notDocumentedSkills
    val missingComparisons = missing.map { SkillComparison(it, null, assessments[it]?.requiredLevel, assessments[it]) }
    val courses = analysis.recommendations.flatMap { it.courses }.distinctBy { it.id }
    val withoutCourses = analysis.recommendations.filter { it.courses.isEmpty() }.map { it.skillName }
}
