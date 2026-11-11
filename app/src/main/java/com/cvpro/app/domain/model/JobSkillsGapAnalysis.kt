package com.cvpro.app.domain.model

val jobSkillLevels = setOf("beginner", "intermediate", "advanced", "expert")

enum class JobCoverageStatus { COMPLETE, PARTIAL, INSUFFICIENT_INFORMATION }
enum class JobCatalogStatus { CONSULTED, NOT_NEEDED }
enum class SkillCatalogMatch { MATCHED, NOT_FOUND, AMBIGUOUS }

data class DocumentedJobSkill(val name: String, val cvLevel: String)
data class JobSkillCourses(
    val skillName: String,
    val catalogMatch: SkillCatalogMatch,
    val courses: List<CourseRecommendation>,
)

data class JobSkillsGapAnalysis(
    val cvId: String,
    val jobId: String,
    val status: JobCoverageStatus,
    val requiredSkillsCount: Int,
    val documentedMatchesCount: Int,
    val documentedCoveragePercent: Double?,
    val matchedSkills: List<DocumentedJobSkill>,
    val notDocumentedSkills: List<String>,
    val catalogStatus: JobCatalogStatus?,
    val recommendations: List<JobSkillCourses>,
)

enum class JobSkillsGapFailure {
    INVALID_CV_LEVEL, INVALID_INPUT, UNAUTHORIZED, ROUTE_UNAVAILABLE, UNAVAILABLE, CATALOG_UNAVAILABLE, INCOMPATIBLE_RESPONSE,
}
class JobSkillsGapException(val failure: JobSkillsGapFailure) : Exception(failure.name)
