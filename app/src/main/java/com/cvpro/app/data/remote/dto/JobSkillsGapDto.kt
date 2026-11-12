package com.cvpro.app.data.remote.dto

data class JobSkillsGapRequestDto(val cv: JobGapCvDto, val jobs: List<JobGapOfferDto>)
data class JobGapCvDto(val id: String, val skills: List<JobGapCvSkillDto>)
data class JobGapCvSkillDto(val name: String, val level: String)
data class JobGapOfferDto(val id: String, val skills: List<String>)

// Los campos de red son nullable para detectar respuestas incompletas de APIM.
data class JobSkillsGapResponseDto(
    val cvId: String?, val dataSource: String?, val results: List<JobGapResultDto>?,
    val catalogStatus: String? = null,
)
data class JobGapResultDto(
    val jobId: String?, val status: String?, val requiredSkillsCount: Int?,
    val documentedMatchesCount: Int?, val documentedCoveragePercent: Double?,
    val matchedSkills: List<JobGapMatchDto>?, val notDocumentedSkills: List<String>?,
    val recommendations: List<JobSkillCoursesDto>? = null,
    val skillLevelAssessments: List<EvaluationAssessmentDto>? = null, val levelAnalysis: EvaluationLevelsDto? = null,
)
data class JobGapMatchDto(val name: String?, val cvLevel: String?)
data class JobSkillCoursesDto(val skillName: String?, val catalogMatch: String?, val courses: List<CourseDto>?)

data class JobSkillsGapEvaluationRequestDto(val cv: JobGapCvDto, val jobs: List<EvaluationJobDto>)
data class EvaluationJobDto(
    val id: String, val skills: List<String>, val description: String?, val requirements: List<String>?,
)

data class EvaluationAssessmentDto(
    val name: String?, val cvLevel: String?, val requiredLevel: String?,
    val evidenceSource: String?, val evidenceText: String?, val detectionStatus: String?,
    val reason: String?, val status: String?, val deficitSteps: Int?,
)
data class EvaluationLevelsDto(
    val scale: String?, val comparedSkillsCount: Int?, val belowRequiredCount: Int?,
    val meetsRequiredCount: Int?, val notDocumentedCount: Int?,
    val unknownRequiredLevelCount: Int?, val conflictCount: Int?,
)
