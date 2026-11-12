package com.cvpro.app.data.mapper

import com.cvpro.app.data.remote.dto.*
import com.cvpro.app.domain.model.*

private fun String.validText(max: Int, multiline: Boolean = false): Boolean =
    codePointCount(0, length) <= max && codePoints().allMatch {
        it !in 0xD800..0xDFFF && (!Character.isISOControl(it) || (multiline && it in listOf(9, 10, 13)))
    }

// ECMAScript trim (used by the backend) also treats the BOM as whitespace.
private fun String.contractBlank(): Boolean = all { it.isWhitespace() || it == '\uFEFF' }

/** Reject the whole CV rather than dropping skills or guessing unknown levels. Empty CV is valid. */
fun Cv.toEvaluationRequest(offer: JobOffer): JobGapEvaluationRequestDto {
    if (skills.size > 500 || skills.any {
        it.name.contractBlank() || !it.name.validText(200, multiline = true) || SkillLevel.fromApi(it.level) == null
    }) throw JobGapException(JobGapFailure.INVALID_CV)
    if (id.contractBlank() || !id.validText(200) || offer.id.contractBlank() || !offer.id.validText(200) ||
        offer.skills.size > 200 || offer.skills.any { !it.validText(200, multiline = true) } ||
        !offer.description.validText(20_000, multiline = true) || offer.requirements.size > 100 ||
        offer.requirements.any { !it.validText(2_000, multiline = true) }
    ) throw JobGapException(JobGapFailure.INVALID_INPUT)
    return JobGapEvaluationRequestDto(
        EvaluationCvDto(id, skills.map { EvaluationCvSkillDto(it.name, SkillLevel.fromApi(it.level)!!.apiValue) }),
        listOf(EvaluationJobDto(offer.id, offer.skills, offer.description, offer.requirements)),
    )
}

private fun compatible(condition: Boolean) {
    if (!condition) throw JobGapException(JobGapFailure.INCOMPATIBLE)
}

private fun String?.level(): SkillLevel? {
    val level = this?.let(SkillLevel::fromApi)
    compatible(this == null || level != null)
    return level
}

fun JobGapEvaluationResponseDto.toEvaluation(cvId: String, jobId: String): JobGapEvaluation {
    compatible(this.cvId == cvId && dataSource == "client_request" && results?.size == 1)
    val result = results!!.single()
    compatible(result.jobId == jobId && result.status in setOf("complete", "partial", "insufficient_information"))
    val required = result.requiredSkillsCount
    val count = result.documentedMatchesCount
    compatible(required != null && required in 0..200 && count != null && count in 0..required)
    compatible(result.matchedSkills != null && result.notDocumentedSkills != null)
    val matches = result.matchedSkills!!.map {
        compatible(!it.name.isNullOrBlank() && it.cvLevel != null)
        DocumentedSkill(it.name!!, it.cvLevel.level()!!)
    }
    val missing = result.notDocumentedSkills!!
    compatible(missing.all { it.isNotBlank() } && count == matches.size && required == count + missing.size)
    val percent = result.documentedCoveragePercent
    compatible(if (required == 0) percent == null && result.status == "insufficient_information"
        else percent != null && percent.isFinite() && percent in 0.0..100.0 &&
            result.status == if (missing.isEmpty()) "complete" else "partial")
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
    return JobGapEvaluation(cvId, jobId, result.status!!, required!!, count!!, percent, matches, missing, assessments, levels)
}
