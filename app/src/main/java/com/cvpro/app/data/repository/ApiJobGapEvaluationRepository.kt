package com.cvpro.app.data.repository

import com.cvpro.app.data.mapper.*
import com.cvpro.app.data.remote.api.JobApi
import com.cvpro.app.data.remote.api.SkillsGapApi
import com.cvpro.app.domain.model.*
import com.cvpro.app.domain.repository.JobGapEvaluationRepository
import com.google.gson.Gson
import com.google.gson.JsonParseException
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiJobGapEvaluationRepository @Inject constructor(
    private val api: SkillsGapApi,
    private val jobs: JobApi,
) : JobGapEvaluationRepository {
    override suspend fun evaluate(cv: Cv, offer: JobOffer): JobGapEvaluation {
        // Validate before any request. The list may contain only a projection of the offer.
        cv.toEvaluationRequest(offer)
        try {
            val completeOffer = if (offer.skills.isEmpty() || offer.description.isBlank() || offer.requirements.isEmpty()) {
                val detail = jobs.getJobById(offer.id)
                if (detail.id != offer.id) throw JobGapException(JobGapFailure.INCOMPATIBLE)
                offer.copy(skills = detail.skills ?: offer.skills,
                    description = detail.description ?: offer.description,
                    requirements = detail.requirements ?: offer.requirements)
            } else offer
            val input = cv.toEvaluationRequest(completeOffer)
            // Gson is also Retrofit's converter; this measures the actual UTF-8 JSON, including escaping.
            if (Gson().toJson(input).toByteArray(Charsets.UTF_8).size > 100 * 1024)
                throw JobGapException(JobGapFailure.TOO_LARGE)
            val response = api.evaluate(input)
            if (response.code() != 200) {
                response.errorBody()?.close()
                throw JobGapException(jobGapHttpFailure(response.code()))
            }
            return response.body()?.toEvaluation(cv.id, offer.id)
                ?: throw JobGapException(JobGapFailure.INCOMPATIBLE)
        } catch (error: HttpException) {
            throw JobGapException(jobGapHttpFailure(error.code()))
        } catch (_: JsonParseException) {
            throw JobGapException(JobGapFailure.INCOMPATIBLE)
        }
    }
}

internal fun jobGapHttpFailure(code: Int): JobGapFailure = when (code) {
    400 -> JobGapFailure.INVALID_INPUT
    401 -> JobGapFailure.UNAUTHORIZED
    403 -> JobGapFailure.FORBIDDEN
    404, 405, 501 -> JobGapFailure.NOT_FOUND
    413 -> JobGapFailure.TOO_LARGE
    else -> JobGapFailure.UNAVAILABLE
}
