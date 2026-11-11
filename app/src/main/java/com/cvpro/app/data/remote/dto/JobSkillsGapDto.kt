package com.cvpro.app.data.remote.dto

data class JobSkillsGapRequestDto(val cv: JobGapCvDto, val jobs: List<JobGapOfferDto>)
data class JobGapCvDto(val id: String, val skills: List<JobGapCvSkillDto>)
data class JobGapCvSkillDto(val name: String, val level: String)
data class JobGapOfferDto(val id: String, val skills: List<String>)

// Los campos de red son nullable para detectar respuestas incompletas de APIM.
data class JobSkillsGapResponseDto(
    val cvId: String?, val dataSource: String?, val results: List<JobGapResultDto>?,
    val catalogStatus: String?,
)
data class JobGapResultDto(
    val jobId: String?, val status: String?, val requiredSkillsCount: Int?,
    val documentedMatchesCount: Int?, val documentedCoveragePercent: Double?,
    val matchedSkills: List<JobGapMatchDto>?, val notDocumentedSkills: List<String>?,
    val recommendations: List<JobSkillCoursesDto>?,
)
data class JobGapMatchDto(val name: String?, val cvLevel: String?)
data class JobSkillCoursesDto(val skillName: String?, val catalogMatch: String?, val courses: List<CourseDto>?)
