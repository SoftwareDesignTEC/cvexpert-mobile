package com.cvpro.app.domain.model

/** Positions on cv-skill-v1 (1–4). The API returns only skills with a calculable deficit. */
data class SkillGapItem(
    val name: String,
    val userLevel: Int,
    val marketLevel: Int,
    val deficitSteps: Int,
)

data class UnresolvedSkill(
    val skillName: String,
    val reason: String,
    val userLevel: Int?,
    val marketLevel: Int?,
)

data class SkillsGapAnalysis(
    val skills: List<SkillGapItem>,
    val recommendations: List<CourseRecommendation>,
    val status: String,
    val comparedSkills: Int,
    val scaleKey: String,
    val marketStatus: String,
    val unresolved: List<UnresolvedSkill>,
    val page: Long,
    val pageSize: Int,
    val totalRecords: Long,
) {
    val hasMore get() = page * pageSize < totalRecords
    val hasFullCoverage get() = status == "complete" && marketStatus == "available" &&
        comparedSkills > 0 && unresolved.isEmpty()
    val canConfirmNoGaps get() = hasFullCoverage && totalRecords == 0L
}

data class CourseRecommendation(
    val id: String,
    val title: String,
    val platform: String,
    val url: String,
    val logoUrl: String?,
    val durationHours: Int?,
)

data class SkillInput(val skillName: String, val level: SkillLevel?)

fun List<SkillInput>.isValidSkillsGapInput(): Boolean =
    size in 1..500 && distinctBy { it.skillName }.size == size && all {
        val name = it.skillName
        name.length in 1..200 && name.isNotBlank() && name == name.trim() && '\u0000' !in name
    }

enum class SkillsGapFailure {
    UNAUTHORIZED, INPUT_REQUIRED, UNAVAILABLE, INVALID_INPUT, INCOMPATIBLE_RESPONSE,
}

class SkillsGapException(val failure: SkillsGapFailure) : Exception(failure.name)
