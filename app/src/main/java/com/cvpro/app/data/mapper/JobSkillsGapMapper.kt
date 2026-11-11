package com.cvpro.app.data.mapper

import com.cvpro.app.data.remote.dto.*
import com.cvpro.app.domain.model.*
import java.net.URI

fun Cv.toJobSkillsGapRequest(offer: JobOffer): JobSkillsGapRequestDto {
    if (skills.any { it.level !in jobSkillLevels }) {
        throw JobSkillsGapException(JobSkillsGapFailure.INVALID_CV_LEVEL)
    }
    fun validText(value: String, allowBlank: Boolean = false): Boolean =
        (allowBlank || value.isNotBlank()) && value.codePointCount(0, value.length) <= 200 &&
            value.none { it.isISOControl() && !it.isWhitespace() }
    if (!validText(id) || !validText(offer.id) || id.any(Char::isISOControl) ||
        offer.id.any(Char::isISOControl) || skills.size > 500 || offer.skills.size > 200 ||
        skills.any { !validText(it.name) } || offer.skills.any { !validText(it, allowBlank = true) }
    ) throw JobSkillsGapException(JobSkillsGapFailure.INVALID_INPUT)
    return JobSkillsGapRequestDto(
        cv = JobGapCvDto(id, skills.map { JobGapCvSkillDto(it.name, it.level) }),
        jobs = listOf(JobGapOfferDto(offer.id, offer.skills)),
    )
}

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
    return JobSkillsGapAnalysis(cvId, jobId, status, required!!, count!!, percent, matched, missing, catalog, recommendations)
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
