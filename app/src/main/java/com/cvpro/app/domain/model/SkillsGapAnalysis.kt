package com.cvpro.app.domain.model

/** cv-skill-v1: ordinal positions, never percentages. GET items contain deficits only. */
data class SkillGapItem(
    val name: String, val userLevel: Int, val marketLevel: Int,
    val deficitSteps: Int = marketLevel - userLevel,
)
data class UnresolvedSkill(
    val skillName: String, val reason: String, val userLevel: Int?, val marketLevel: Int?,
)
data class SkillsGapAnalysis(
    val skills: List<SkillGapItem>, val recommendations: List<CourseRecommendation>,
    val status: String = "incomplete", val comparedSkills: Int = 0,
    val scaleKey: String = "cv-skill-v1", val marketStatus: String = "missing",
    val unresolved: List<UnresolvedSkill> = emptyList(),
    val page: Int = 1, val pageSize: Int = 100, val totalRecords: Int = skills.size,
) {
    val skillsToImprove get() = skills.sortedByDescending { it.deficitSteps }
    val hasMore get() = page.toLong() * pageSize < totalRecords
    val canConfirmNoGaps get() = status == "complete" && marketStatus == "available" &&
        comparedSkills > 0 && unresolved.isEmpty() && totalRecords == 0
}
data class CourseRecommendation(
    val id: String, val title: String, val platform: String, val url: String,
    val logoUrl: String?, val duration: String?,
)
data class SkillInput(val skillName: String, val level: String?)
enum class SkillsGapFailure {
    DISABLED, UNAUTHORIZED, INPUT_REQUIRED, UNAVAILABLE, INVALID_INPUT, INCOMPATIBLE_RESPONSE,
}
class SkillsGapException(val failure: SkillsGapFailure) : Exception(failure.name)
