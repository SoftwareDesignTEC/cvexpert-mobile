package com.cvpro.app.data.repository

import com.cvpro.app.data.mapper.toDomain
import com.cvpro.app.data.mapper.toInputDto
import com.cvpro.app.data.remote.api.SkillsGapApi
import com.cvpro.app.data.remote.dto.SkillsGapErrorDto
import com.cvpro.app.domain.model.SkillInput
import com.cvpro.app.domain.model.SkillsGapAnalysis
import com.cvpro.app.domain.model.SkillsGapException
import com.cvpro.app.domain.model.SkillsGapFailure
import com.cvpro.app.domain.model.isValidSkillsGapInput
import com.cvpro.app.domain.repository.SkillsGapRepository
import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiSkillsGapRepository @Inject constructor(
    private val api: SkillsGapApi,
) : SkillsGapRepository {
    override suspend fun getAnalysis(page: Long): SkillsGapAnalysis {
        val response = api.getSkillsGap(page, PAGE_SIZE)
        response.checkStatus(isRead = true)
        val analysis = response.body()?.toDomain()
        if (response.code() != 200 || response.headers()["X-Data-Mode"] != "reference" ||
            analysis == null || analysis.page != page
        ) {
            throw SkillsGapException(SkillsGapFailure.INCOMPATIBLE_RESPONSE)
        }
        return analysis
    }

    override suspend fun replaceInput(skills: List<SkillInput>) {
        if (!skills.isValidSkillsGapInput()) throw SkillsGapException(SkillsGapFailure.INVALID_INPUT)

        // Keep explicit null levels on this endpoint without changing the shared Retrofit Gson.
        val json = gson.toJson(skills.toInputDto()).toByteArray(Charsets.UTF_8)
        if (json.size > 100 * 1024) throw SkillsGapException(SkillsGapFailure.INVALID_INPUT)

        val response = api.putInput(json.toRequestBody("application/json; charset=utf-8".toMediaType()))
        response.checkStatus()
        if (response.code() != 204) throw SkillsGapException(SkillsGapFailure.INCOMPATIBLE_RESPONSE)
    }

    private fun Response<*>.checkStatus(isRead: Boolean = false) {
        if (isSuccessful) return
        val errorCode = errorBody()?.use {
            if (code() == 409 && isRead) {
                try {
                    gson.fromJson(it.charStream(), SkillsGapErrorDto::class.java)?.error
                } catch (_: JsonParseException) {
                    null
                }
            } else {
                null
            }
        }
        val failure = when {
            code() == 401 -> SkillsGapFailure.UNAUTHORIZED
            code() == 409 && errorCode == "SKILLS_INPUT_REQUIRED" -> SkillsGapFailure.INPUT_REQUIRED
            !isRead && code() in setOf(400, 413) -> SkillsGapFailure.INVALID_INPUT
            else -> SkillsGapFailure.UNAVAILABLE
        }
        throw SkillsGapException(failure)
    }

    private companion object {
        const val PAGE_SIZE = 100
        val gson = GsonBuilder().serializeNulls().create()
    }
}
