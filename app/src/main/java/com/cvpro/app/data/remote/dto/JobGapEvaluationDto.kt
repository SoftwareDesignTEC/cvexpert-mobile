package com.cvpro.app.data.remote.dto

data class JobGapEvaluationRequestDto(val cv: EvaluationCvDto, val jobs: List<EvaluationJobDto>)
data class EvaluationCvDto(val id: String, val skills: List<EvaluationCvSkillDto>)
data class EvaluationCvSkillDto(val name: String, val level: String)
data class EvaluationJobDto(
    val id: String, val skills: List<String>, val description: String?, val requirements: List<String>?,
)

// Nullable network fields let the mapper reject incomplete responses without inventing values.
data class JobGapEvaluationResponseDto(
    val cvId: String?, val dataSource: String?, val results: List<EvaluationResultDto>?,
)
data class EvaluationResultDto(
    val jobId: String?, val status: String?, val requiredSkillsCount: Int?,
    val documentedMatchesCount: Int?, val documentedCoveragePercent: Double?,
    val matchedSkills: List<EvaluationMatchDto>?, val notDocumentedSkills: List<String>?,
    val skillLevelAssessments: List<EvaluationAssessmentDto>?, val levelAnalysis: EvaluationLevelsDto?,
)
data class EvaluationMatchDto(val name: String?, val cvLevel: String?)
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
