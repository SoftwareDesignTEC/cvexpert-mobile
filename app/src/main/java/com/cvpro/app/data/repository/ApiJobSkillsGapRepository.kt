package com.cvpro.app.data.repository

import com.cvpro.app.data.mapper.toJobAnalysis
import com.cvpro.app.data.mapper.toJobSkillsGapRequest
import com.cvpro.app.data.remote.api.SkillsGapApi
import com.cvpro.app.domain.model.*
import com.cvpro.app.domain.repository.JobSkillsGapRepository
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiJobSkillsGapRepository @Inject constructor(private val api: SkillsGapApi) : JobSkillsGapRepository {
    override suspend fun analyze(cv: Cv, offer: JobOffer, evaluationOnly: Boolean): JobSkillsGapAnalysis {
        val input = cv.toJobSkillsGapRequest(offer)
        if (gson.toJson(input).toByteArray(Charsets.UTF_8).size > 100 * 1024) {
            throw JobSkillsGapException(JobSkillsGapFailure.INVALID_INPUT)
        }
        try {
            val response = if (evaluationOnly) api.evaluateForJob(input) else api.recommendForJob(input)
            if (response.code() != 200) {
                val code = response.errorBody()?.use {
                    try { gson.fromJson(it.charStream(), JsonObject::class.java)?.get("error")?.asString }
                    catch (_: RuntimeException) { null }
                }
                val failure = when {
                    response.code() == 401 -> JobSkillsGapFailure.UNAUTHORIZED
                    response.code() in setOf(404, 405, 501) -> JobSkillsGapFailure.ROUTE_UNAVAILABLE
                    response.code() in setOf(400, 413) -> JobSkillsGapFailure.INVALID_INPUT
                    !evaluationOnly && response.code() == 503 && code == "CATALOG_UNAVAILABLE" ->
                        JobSkillsGapFailure.CATALOG_UNAVAILABLE
                    else -> JobSkillsGapFailure.UNAVAILABLE
                }
                throw JobSkillsGapException(failure)
            }
            return response.body()?.toJobAnalysis(cv.id, offer.id, evaluationOnly)
                ?: throw JobSkillsGapException(JobSkillsGapFailure.INCOMPATIBLE_RESPONSE)
        } catch (_: JsonParseException) {
            throw JobSkillsGapException(JobSkillsGapFailure.INCOMPATIBLE_RESPONSE)
        }
    }

    private companion object { val gson = Gson() }
}
