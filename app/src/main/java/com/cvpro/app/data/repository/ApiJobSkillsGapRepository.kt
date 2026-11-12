package com.cvpro.app.data.repository

import com.cvpro.app.data.mapper.toJobAnalysis
import com.cvpro.app.data.mapper.toJobSkillsGapRequest
import com.cvpro.app.data.mapper.toEvaluationRequest
import com.cvpro.app.data.remote.api.JobApi
import com.cvpro.app.data.remote.api.SkillsGapApi
import com.cvpro.app.domain.model.*
import com.cvpro.app.domain.repository.JobSkillsGapRepository
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.HttpException

@Singleton
class ApiJobSkillsGapRepository @Inject constructor(
    private val api: SkillsGapApi,
    private val jobs: JobApi,
) : JobSkillsGapRepository {
    override suspend fun analyze(cv: Cv, offer: JobOffer, evaluationOnly: Boolean): JobSkillsGapAnalysis {
        cv.toJobSkillsGapRequest(offer)
        try {
            val fullOffer = if (offer.skills.isEmpty() || offer.description.isBlank() || offer.requirements.isEmpty()) {
                val detail = jobs.getJobById(offer.id)
                if (detail.id != offer.id) throw JobSkillsGapException(JobSkillsGapFailure.INCOMPATIBLE_RESPONSE)
                offer.copy(skills = detail.skills ?: offer.skills, description = detail.description ?: offer.description,
                    requirements = detail.requirements ?: offer.requirements)
            } else offer
            val response = if (evaluationOnly) {
                val input = cv.toEvaluationRequest(fullOffer)
                checkSize(input, JobSkillsGapFailure.TOO_LARGE)
                api.evaluateForJob(input)
            } else {
                // This endpoint rejects description/requirements: keep its closed contract unchanged.
                val input = cv.toJobSkillsGapRequest(fullOffer)
                checkSize(input, JobSkillsGapFailure.INVALID_INPUT)
                api.recommendForJob(input)
            }
            if (response.code() != 200) {
                val code = response.errorBody()?.use {
                    try { gson.fromJson(it.charStream(), JsonObject::class.java)?.get("error")?.asString }
                    catch (_: RuntimeException) { null }
                }
                val failure = when {
                    response.code() == 401 -> JobSkillsGapFailure.UNAUTHORIZED
                    response.code() == 403 -> JobSkillsGapFailure.FORBIDDEN
                    response.code() in setOf(404, 405, 501) -> JobSkillsGapFailure.ROUTE_UNAVAILABLE
                    response.code() == 413 -> JobSkillsGapFailure.TOO_LARGE
                    response.code() == 400 -> JobSkillsGapFailure.INVALID_INPUT
                    !evaluationOnly && response.code() == 503 && code == "CATALOG_UNAVAILABLE" ->
                        JobSkillsGapFailure.CATALOG_UNAVAILABLE
                    else -> JobSkillsGapFailure.UNAVAILABLE
                }
                throw JobSkillsGapException(failure)
            }
            return response.body()?.toJobAnalysis(cv.id, offer.id, evaluationOnly)
                ?: throw JobSkillsGapException(JobSkillsGapFailure.INCOMPATIBLE_RESPONSE)
        } catch (error: HttpException) {
            throw JobSkillsGapException(when (error.code()) {
                401 -> JobSkillsGapFailure.UNAUTHORIZED
                403 -> JobSkillsGapFailure.FORBIDDEN
                404 -> JobSkillsGapFailure.ROUTE_UNAVAILABLE
                400 -> JobSkillsGapFailure.INVALID_INPUT
                413 -> JobSkillsGapFailure.TOO_LARGE
                else -> JobSkillsGapFailure.UNAVAILABLE
            })
        } catch (_: JsonParseException) {
            throw JobSkillsGapException(JobSkillsGapFailure.INCOMPATIBLE_RESPONSE)
        }
    }

    private fun checkSize(input: Any, failure: JobSkillsGapFailure) {
        if (gson.toJson(input).toByteArray(Charsets.UTF_8).size > 100 * 1024)
            throw JobSkillsGapException(failure)
    }

    private companion object { val gson = Gson() }
}
