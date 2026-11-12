package com.cvpro.app.domain.model

data class JobGapEvaluation(
    val cvId: String, val jobId: String, val status: String,
    val requiredSkillsCount: Int, val documentedMatchesCount: Int,
    val documentedCoveragePercent: Double?, val matchedSkills: List<DocumentedSkill>,
    val notDocumentedSkills: List<String>, val assessments: List<JobLevelAssessment>?,
    val levelAnalysis: JobLevelAnalysis?,
)
data class DocumentedSkill(val name: String, val level: SkillLevel)
data class JobLevelAssessment(
    val name: String, val cvLevel: SkillLevel?, val requiredLevel: SkillLevel?,
    val evidenceSource: String?, val evidenceText: String?, val detectionStatus: String,
    val reason: String?, val status: String, val deficitSteps: Int?,
)
data class JobLevelAnalysis(
    val compared: Int, val below: Int, val meets: Int, val notDocumented: Int,
    val unknownRequired: Int, val conflicts: Int,
)
enum class JobGapFailure { INVALID_CV, INVALID_INPUT, UNAUTHORIZED, FORBIDDEN, NOT_FOUND, TOO_LARGE, UNAVAILABLE, INCOMPATIBLE }
class JobGapException(val failure: JobGapFailure) : Exception(failure.name)
