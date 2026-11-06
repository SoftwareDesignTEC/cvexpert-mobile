package com.cvpro.app.data.remote.dto

// Nullable wire integers prevent Gson from silently replacing missing fields with zero.
data class SkillsGapPageDto(
    val items: List<SkillsGapDto>?, val page: Int?, val pageSize: Int?,
    val totalRecords: Int?, val analysis: GapAnalysisDto?,
)
data class GapAnalysisDto(
    val status: String?, val comparedSkills: Int?, val scaleKey: String?,
    val marketStatus: String?, val unresolved: List<UnresolvedSkillDto>?,
)
data class UnresolvedSkillDto(
    val skillName: String, val reason: String, val userLevel: Int?, val marketLevel: Int?,
)
data class SkillsGapDto(
    val skillName: String, val userLevel: Int?, val marketLevel: Int?,
    val deficitSteps: Int?, val courses: List<CourseDto>?,
)
data class CourseDto(
    val id: String, val title: String, val url: String, val platform: String,
    val logoUrl: String?, val durationHours: Int?,
    val isPaid: Boolean? = null, val skills: List<String>? = null,
)
