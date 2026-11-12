package com.cvpro.app.data.mapper

import com.cvpro.app.data.remote.dto.*
import com.cvpro.app.domain.model.*
import java.net.URI

fun Cv.toJobSkillsGapRequest(offer: JobOffer): JobSkillsGapRequestDto {
    validateJobGapInput(offer, includeText = false)
    return JobSkillsGapRequestDto(JobGapCvDto(id, skills.map { JobGapCvSkillDto(it.name, it.level) }),
        listOf(JobGapOfferDto(offer.id, offer.skills)))
}

fun Cv.toEvaluationRequest(offer: JobOffer): JobSkillsGapEvaluationRequestDto {
    validateJobGapInput(offer, includeText = true)
    return JobSkillsGapEvaluationRequestDto(JobGapCvDto(id, skills.map { JobGapCvSkillDto(it.name, it.level) }),
        listOf(EvaluationJobDto(offer.id, offer.skills, offer.description, offer.requirements)))
}

private fun Cv.validateJobGapInput(offer: JobOffer, includeText: Boolean) {
    if (skills.any { SkillLevel.fromApi(it.level) == null })
        throw JobSkillsGapException(JobSkillsGapFailure.INVALID_CV_LEVEL)
    if (skills.size > 500 || skills.any { it.name.contractBlank() || !it.name.validText(200, true) })
        throw JobSkillsGapException(JobSkillsGapFailure.INVALID_CV)
    if (id.contractBlank() || !id.validText(200) || offer.id.contractBlank() || !offer.id.validText(200) ||
        offer.skills.size > 200 || offer.skills.any { !it.validText(200, true) } ||
        (includeText && (!offer.description.validText(20_000, true) || offer.requirements.size > 100 ||
            offer.requirements.any { !it.validText(2_000, true) })))
        throw JobSkillsGapException(JobSkillsGapFailure.INVALID_INPUT)
}

private fun String.validText(max: Int, multiline: Boolean = false): Boolean =
    codePointCount(0, length) <= max && codePoints().allMatch {
        it !in 0xD800..0xDFFF && (!Character.isISOControl(it) || (multiline && it in listOf(9, 10, 13)))
    }

// ECMAScript trim (used by the backend) also treats the BOM as whitespace.
private fun String.contractBlank(): Boolean = all { it.isWhitespace() || it == '\uFEFF' }

private fun compatible(value: Boolean) {
    if (!value) throw JobSkillsGapException(JobSkillsGapFailure.INCOMPATIBLE_RESPONSE)
}

private fun String?.httpUrl(): Boolean = try {
    val uri = URI(this ?: "")
    uri.scheme in setOf("http", "https") && !uri.host.isNullOrBlank() && uri.userInfo == null
} catch (_: Exception) { false }

fun JobSkillsGapResponseDto.toJobAnalysis(cvId: String, jobId: String, evaluationOnly: Boolean): JobSkillsGapAnalysis {
    compatible(this.cvId == cvId && dataSource == "client_request" && results?.size == 1)
    val result = results!!.single()
    compatible(result.jobId == jobId)
    val status = when (result.status) {
        "complete" -> JobCoverageStatus.COMPLETE
        "partial" -> JobCoverageStatus.PARTIAL
        "insufficient_information" -> JobCoverageStatus.INSUFFICIENT_INFORMATION
        else -> throw JobSkillsGapException(JobSkillsGapFailure.INCOMPATIBLE_RESPONSE)
    }
    val required = result.requiredSkillsCount
    val count = result.documentedMatchesCount
    compatible(required != null && required in 0..200 && count != null && count in 0..required)
    compatible(result.matchedSkills != null && result.notDocumentedSkills != null)
    val matched = result.matchedSkills!!.map {
        compatible(!it.name.isNullOrBlank() && it.cvLevel in jobSkillLevels)
        DocumentedJobSkill(it.name!!, it.cvLevel!!)
    }
    val missing = result.notDocumentedSkills!!
    compatible(missing.all { it.isNotBlank() } && missing.distinct().size == missing.size)
    compatible(matched.map { it.name }.distinct().size == matched.size)
    compatible(count == matched.size && required == count + missing.size)
    val percent = result.documentedCoveragePercent
    if (status == JobCoverageStatus.INSUFFICIENT_INFORMATION) {
        compatible(required == 0 && percent == null)
    } else {
        compatible(required!! > 0 && percent != null && percent.isFinite() && percent in 0.0..100.0)
        compatible(kotlin.math.abs(percent!! - count!! * 100.0 / required) <= 0.0051)
        compatible((status == JobCoverageStatus.COMPLETE) == missing.isEmpty())
    }
    val catalog = if (evaluationOnly) null else when (catalogStatus) {
        "consulted" -> JobCatalogStatus.CONSULTED
        "not_needed" -> JobCatalogStatus.NOT_NEEDED
        else -> throw JobSkillsGapException(JobSkillsGapFailure.INCOMPATIBLE_RESPONSE)
    }
    val recommendations = if (evaluationOnly) emptyList() else {
        compatible(result.recommendations != null)
        compatible(result.recommendations!!.map { it.skillName } == missing)
        compatible((catalog == JobCatalogStatus.NOT_NEEDED) == missing.isEmpty())
        result.recommendations.map { it.toJobCourses() }
    }
    val assessments = result.skillLevelAssessments?.map {
        compatible(!it.name.isNullOrBlank() && it.detectionStatus in setOf("explicit", "unknown", "conflict") &&
            it.status in setOf("documented_skill_below_required", "documented_skill_meets_required", "not_documented", "unknown_required_level") &&
            (it.deficitSteps == null || it.deficitSteps in 0..3) &&
            (it.reason == null || it.reason in setOf("no_explicit_evidence", "optional", "negated", "contradictory", "ambiguous")))
        val cvLevel = it.cvLevel.level()
        val requiredLevel = it.requiredLevel.level()
        compatible(if (it.detectionStatus == "explicit") requiredLevel != null &&
            it.evidenceSource in setOf("description", "requirements") && it.evidenceText != null
            else requiredLevel == null && it.evidenceSource == null && it.evidenceText == null)
        compatible(if (it.status == "not_documented") cvLevel == null && it.deficitSteps == null
            else cvLevel != null)
        compatible(if (it.status == "unknown_required_level") requiredLevel == null && it.deficitSteps == null
            else true)
        compatible(if (it.status!!.startsWith("documented_skill_")) requiredLevel != null &&
            it.deficitSteps != null && (if (it.status.endsWith("below_required")) it.deficitSteps > 0 else it.deficitSteps == 0)
            else true)
        JobLevelAssessment(it.name!!, cvLevel, requiredLevel, it.evidenceSource, it.evidenceText,
            it.detectionStatus!!, it.reason, it.status, it.deficitSteps)
    }
    val levels = result.levelAnalysis?.let {
        compatible(it.scale == "cv-skill-v1" && listOf(it.comparedSkillsCount, it.belowRequiredCount,
            it.meetsRequiredCount, it.notDocumentedCount, it.unknownRequiredLevelCount, it.conflictCount)
            .all { count -> count != null && count in 0..200 })
        JobLevelAnalysis(it.comparedSkillsCount!!, it.belowRequiredCount!!, it.meetsRequiredCount!!,
            it.notDocumentedCount!!, it.unknownRequiredLevelCount!!, it.conflictCount!!)
    }
    // Both fields are intentionally absent in the legacy, coverage-only response.
    compatible((assessments == null) == (levels == null))
    compatible(assessments == null || assessments.size == required)
    return JobSkillsGapAnalysis(cvId, jobId, status, required!!, count!!, percent, matched, missing, catalog, recommendations, assessments, levels)
}

private fun JobSkillCoursesDto.toJobCourses(): JobSkillCourses {
    compatible(!skillName.isNullOrBlank() && courses != null && courses.size <= 5)
    val match = when (catalogMatch) {
        "matched" -> SkillCatalogMatch.MATCHED
        "not_found" -> SkillCatalogMatch.NOT_FOUND
        "ambiguous" -> SkillCatalogMatch.AMBIGUOUS
        else -> throw JobSkillsGapException(JobSkillsGapFailure.INCOMPATIBLE_RESPONSE)
    }
    compatible(match == SkillCatalogMatch.MATCHED || courses!!.isEmpty())
    compatible(courses!!.map { it.id }.distinct().size == courses.size)
    return JobSkillCourses(skillName!!, match, courses.map {
        compatible(!it.id.isNullOrBlank() && !it.title.isNullOrBlank() && !it.platform.isNullOrBlank() &&
            it.url.httpUrl() && (it.logoUrl == null || it.logoUrl.httpUrl()) &&
            (it.durationHours == null || it.durationHours >= 0) && it.isPaid != null && it.skills != null)
        CourseRecommendation(it.id, it.title, it.platform, it.url, it.logoUrl,
            it.durationHours?.let { hours -> "$hours horas" }, it.isPaid, it.skills!!)
    })
}

private fun String?.level(): SkillLevel? {
    val level = this?.let(SkillLevel::fromApi)
    compatible(this == null || level != null)
    return level
}
