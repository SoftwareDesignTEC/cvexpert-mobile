package com.cvpro.app.data.remote.dto

data class SkillsGapInputDto(val skills: List<SkillInputDto>)

data class SkillInputDto(val skillName: String, val level: String?)

// Nullable wire fields let the mapper reject legacy or incomplete payloads.
data class SkillsGapPageDto(
    val items: List<SkillsGapDto>?,
    val page: Long?,
    val pageSize: Int?,
    val totalRecords: Long?,
    val analysis: GapAnalysisDto?,
)

data class GapAnalysisDto(
    val status: String?,
    val comparedSkills: Int?,
    val scaleKey: String?,
    val marketStatus: String?,
    val unresolved: List<UnresolvedSkillDto>?,
)

data class UnresolvedSkillDto(
    val skillName: String?,
    val reason: String?,
    val userLevel: Int?,
    val marketLevel: Int?,
)

data class SkillsGapDto(
    val skillName: String?,
    val userLevel: Int?,
    val marketLevel: Int?,
    val deficitSteps: Int?,
    val courses: List<CourseDto>?,
)

data class CourseDto(
    val id: String?,
    val title: String?,
    val url: String?,
    val platform: String?,
    val logoUrl: String?,
    val durationHours: Int?,
)

data class SkillsGapErrorDto(val error: String?)
