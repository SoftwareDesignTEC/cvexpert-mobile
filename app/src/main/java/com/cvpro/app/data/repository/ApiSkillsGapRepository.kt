package com.cvpro.app.data.repository

import com.cvpro.app.BuildConfig
import com.cvpro.app.data.mapper.toDomain
import com.cvpro.app.data.mapper.toInputJson
import com.cvpro.app.data.remote.api.SkillsGapApi
import com.cvpro.app.domain.model.*
import com.cvpro.app.domain.repository.SkillsGapRepository
import retrofit2.Response
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiSkillsGapRepository internal constructor(
    private val api: SkillsGapApi,
    override val enabled: Boolean,
) : SkillsGapRepository {
    @Inject constructor(api: SkillsGapApi) : this(api, BuildConfig.SKILLS_GAP_HTTP_020_ENABLED)
    private fun requireEnabled() {
        if (!enabled) throw SkillsGapException(SkillsGapFailure.DISABLED)
    }
    override suspend fun getAnalysis(page: Int): SkillsGapAnalysis {
        requireEnabled()
        val response = api.getSkillsGap(page, 100)
        response.checkStatus(isRead = true)
        if (response.headers()["X-Data-Mode"] != "reference")
            throw SkillsGapException(SkillsGapFailure.INCOMPATIBLE_RESPONSE)
        val result = response.body()?.toDomain()
            ?: throw SkillsGapException(SkillsGapFailure.INCOMPATIBLE_RESPONSE)
        if (result.page != page) throw SkillsGapException(SkillsGapFailure.INCOMPATIBLE_RESPONSE)
        return result
    }
    override suspend fun replaceInput(skills: List<SkillInput>) {
        requireEnabled()
        // The shared Gson omits null properties. Use the exact JSON here so unknown
        // levels are explicitly null without altering serialization of other APIs.
        val json = skills.toInputJson().toString()
        if (json.toByteArray(Charsets.UTF_8).size > 100 * 1024)
            throw SkillsGapException(SkillsGapFailure.INVALID_INPUT)
        val response = api.putInput(json.toRequestBody("application/json; charset=utf-8".toMediaType()))
        response.checkStatus()
        if (response.code() != 204) throw SkillsGapException(SkillsGapFailure.INCOMPATIBLE_RESPONSE)
    }
}
private fun Response<*>.checkStatus(isRead: Boolean = false) {
    if (isSuccessful) return
    val errorCode = errorBody()?.use {
        if (isRead && code() == 409) try {
            com.google.gson.Gson().fromJson(it.charStream(), com.google.gson.JsonObject::class.java)?.get("error")?.asString
        } catch (_: RuntimeException) { null } else null
    }
    throw SkillsGapException(when (code()) {
        401 -> SkillsGapFailure.UNAUTHORIZED
        409 -> if (isRead && errorCode == "SKILLS_INPUT_REQUIRED") SkillsGapFailure.INPUT_REQUIRED else SkillsGapFailure.UNAVAILABLE
        400, 413 -> SkillsGapFailure.INVALID_INPUT
        else -> SkillsGapFailure.UNAVAILABLE
    })
}
