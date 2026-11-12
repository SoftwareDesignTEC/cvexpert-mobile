package com.cvpro.app.data

import com.cvpro.app.data.mapper.toDomain
import com.cvpro.app.data.mapper.toJobAnalysis
import com.cvpro.app.data.mapper.toJobSkillsGapRequest
import com.cvpro.app.data.remote.api.SkillsGapApi
import com.cvpro.app.data.remote.dto.JobOfferDto
import com.cvpro.app.data.remote.dto.JobSkillsGapResponseDto
import com.cvpro.app.data.repository.ApiJobSkillsGapRepository
import com.cvpro.app.domain.model.*
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class JobSkillsGapRepositoryTest {
    private val gson = Gson()

    @Test fun minimalRequestKeepsLevelsAndJobSkillsWithoutPersonalDataOrMarketRequirements() {
        val cv = jobGapCv()
        val dto = JobOfferDto(id = "job1", skills = listOf("Python", "SQL"),
            requirements = listOf("Texto libre"))
        val offer = dto.toDomain()
        assertEquals(listOf("Python", "SQL"), offer.skills)
        val json = gson.toJsonTree(cv.toJobSkillsGapRequest(offer)).asJsonObject
        assertEquals(setOf("cv", "jobs"), json.keySet())
        assertEquals(setOf("id", "skills"), json["cv"].asJsonObject.keySet())
        assertEquals(setOf("name", "level"), json["cv"].asJsonObject["skills"].asJsonArray[0].asJsonObject.keySet())
        assertEquals("advanced", json["cv"].asJsonObject["skills"].asJsonArray[0].asJsonObject["level"].asString)
        assertEquals(setOf("id", "skills"), json["jobs"].asJsonArray[0].asJsonObject.keySet())
        assertFalse(json.toString().contains("private"))
        assertFalse(json.toString().contains("Texto libre"))
    }

    @Test fun unknownLevelsAndInvalidIdsAreExplicitFailuresWhileEmptyCvIsValid() {
        for (level in listOf("", "unknown", "Advanced")) {
            assertFailure(JobSkillsGapFailure.INVALID_CV_LEVEL) {
                jobGapCv(level = level).toJobSkillsGapRequest(jobGapOffer())
            }
        }
        assertFailure(JobSkillsGapFailure.INVALID_INPUT) {
            jobGapCv(id = "").toJobSkillsGapRequest(jobGapOffer())
        }
        assertTrue(jobGapCv().copy(skills = emptyList()).toJobSkillsGapRequest(jobGapOffer()).cv.skills.isEmpty())
    }

    @Test fun responseMappingPreservesDeclaredLevelsUnknownDurationAndCatalogStates() {
        val dto = gson.fromJson(jobGapResponse, JobSkillsGapResponseDto::class.java)
        val analysis = dto.toJobAnalysis("cv1", "job1", false)
        assertEquals(50.0, analysis.documentedCoveragePercent!!, 0.001)
        assertEquals("advanced", analysis.matchedSkills.single().cvLevel)
        assertEquals(SkillCatalogMatch.MATCHED, analysis.recommendations.single().catalogMatch)
        assertNull(analysis.recommendations.single().courses.single().duration)
        assertEquals(false, analysis.recommendations.single().courses.single().isPaid)

        val empty = gson.fromJson(jobGapResponse, JsonObject::class.java)
        empty["results"].asJsonArray[0].asJsonObject["recommendations"].asJsonArray[0].asJsonObject
            .add("courses", gson.toJsonTree(emptyList<Any>()))
        assertTrue(gson.fromJson(empty, JobSkillsGapResponseDto::class.java)
            .toJobAnalysis("cv1", "job1", false).recommendations.single().courses.isEmpty())

        val pure = gson.fromJson(jobGapResponse, JsonObject::class.java)
        pure.remove("catalogStatus")
        pure["results"].asJsonArray[0].asJsonObject.remove("recommendations")
        val evaluation = gson.fromJson(pure, JobSkillsGapResponseDto::class.java)
            .toJobAnalysis("cv1", "job1", true)
        assertNull(evaluation.catalogStatus)
        assertTrue(evaluation.recommendations.isEmpty())
    }

    @Test fun incompatibleBodiesAndMismatchedSelectionNeverBecomeSuccessfulResults() {
        for (change in listOf<(JsonObject) -> Unit>(
            { it.remove("results") },
            { it.addProperty("dataSource", "demo") },
            { it.addProperty("cvId", "another-cv") },
            { it["results"].asJsonArray[0].asJsonObject.remove("matchedSkills") },
            { it["results"].asJsonArray[0].asJsonObject.addProperty("jobId", "another-job") },
            { it["results"].asJsonArray[0].asJsonObject.addProperty("documentedCoveragePercent", 100) },
            { it.remove("catalogStatus") },
        )) {
            val json = gson.fromJson(jobGapResponse, JsonObject::class.java)
            change(json)
            assertFailure(JobSkillsGapFailure.INCOMPATIBLE_RESPONSE) {
                gson.fromJson(json, JobSkillsGapResponseDto::class.java).toJobAnalysis("cv1", "job1", false)
            }
        }
    }

    @Test fun requestsUseBothRelativeApimRoutesAndDistinguishCatalogOutageFromUnpublishedOperation() = runTest {
        val server = MockWebServer()
        server.start()
        try {
            val api = Retrofit.Builder().baseUrl(server.url("/api/v1/"))
                .addConverterFactory(GsonConverterFactory.create()).build().create(SkillsGapApi::class.java)
            val repository = ApiJobSkillsGapRepository(api)
            server.enqueue(MockResponse().setBody(jobGapResponse))
            repository.analyze(jobGapCv(), jobGapOffer(), false)
            val call = server.takeRequest()
            assertEquals("POST", call.method)
            assertEquals("/api/v1/skills-gap/recommendations", call.path)
            val json = gson.fromJson(call.body.readUtf8(), JsonObject::class.java)
            assertEquals("job1", json["jobs"].asJsonArray[0].asJsonObject["id"].asString)

            for ((code, content, failure) in listOf(
                Triple(503, """{"error":"CATALOG_UNAVAILABLE"}""", JobSkillsGapFailure.CATALOG_UNAVAILABLE),
                Triple(503, """{"error":"APIM_SECURITY_UNAVAILABLE"}""", JobSkillsGapFailure.UNAVAILABLE),
                Triple(404, "", JobSkillsGapFailure.ROUTE_UNAVAILABLE),
                Triple(401, "", JobSkillsGapFailure.UNAUTHORIZED),
                Triple(400, "", JobSkillsGapFailure.INVALID_INPUT),
            )) {
                server.enqueue(MockResponse().setResponseCode(code).setBody(content))
                try {
                    repository.analyze(jobGapCv(), jobGapOffer(), false)
                    fail("Se esperaba un error explícito")
                } catch (error: JobSkillsGapException) { assertEquals(failure, error.failure) }
            }
            repeat(5) { server.takeRequest() }
            val pure = gson.fromJson(jobGapResponse, JsonObject::class.java)
            pure.remove("catalogStatus")
            pure["results"].asJsonArray[0].asJsonObject.remove("recommendations")
            server.enqueue(MockResponse().setBody(pure.toString()))
            repository.analyze(jobGapCv(), jobGapOffer(), true)
            assertEquals("/api/v1/skills-gap/evaluate", server.takeRequest().path)
        } finally { server.shutdown() }
    }

    @Test fun insufficientInformationKeepsNullCoverage() {
        val dto = gson.fromJson("""
            {"cvId":"cv1","dataSource":"client_request","catalogStatus":"not_needed","results":[{
              "jobId":"job1","status":"insufficient_information","requiredSkillsCount":0,
              "documentedMatchesCount":0,"documentedCoveragePercent":null,"matchedSkills":[],
              "notDocumentedSkills":[],"recommendations":[]}]}
        """, JobSkillsGapResponseDto::class.java)
        val result = dto.toJobAnalysis("cv1", "job1", false)
        assertEquals(JobCoverageStatus.INSUFFICIENT_INFORMATION, result.status)
        assertNull(result.documentedCoveragePercent)
        assertEquals(JobCatalogStatus.NOT_NEEDED, result.catalogStatus)
    }

    @Test fun completeAndEmptyCvAreValidResultsWithoutInventedMissingSkills() {
        val complete = gson.fromJson("""
            {"cvId":"cv1","dataSource":"client_request","catalogStatus":"not_needed","results":[{
              "jobId":"job1","status":"complete","requiredSkillsCount":1,
              "documentedMatchesCount":1,"documentedCoveragePercent":100,
              "matchedSkills":[{"name":"Python","cvLevel":"beginner"}],
              "notDocumentedSkills":[],"recommendations":[]}]}
        """, JobSkillsGapResponseDto::class.java).toJobAnalysis("cv1", "job1", false)
        assertEquals(JobCoverageStatus.COMPLETE, complete.status)
        assertTrue(complete.recommendations.isEmpty())

        val empty = gson.fromJson(jobGapResponse, JsonObject::class.java)
        val result = empty["results"].asJsonArray[0].asJsonObject
        result.addProperty("requiredSkillsCount", 1)
        result.addProperty("documentedMatchesCount", 0)
        result.addProperty("documentedCoveragePercent", 0)
        result.add("matchedSkills", gson.toJsonTree(emptyList<Any>()))
        val mapped = gson.fromJson(empty, JobSkillsGapResponseDto::class.java).toJobAnalysis("cv1", "job1", false)
        assertEquals(JobCoverageStatus.PARTIAL, mapped.status)
        assertEquals(0.0, mapped.documentedCoveragePercent!!, 0.001)
    }

    @Test fun bodyLimitIsEnforcedBeforeStartingAnHttpRequest() = runTest {
        val server = MockWebServer()
        server.start()
        try {
            val repository = ApiJobSkillsGapRepository(Retrofit.Builder().baseUrl(server.url("/"))
                .addConverterFactory(GsonConverterFactory.create()).build().create(SkillsGapApi::class.java))
            val cv = jobGapCv().copy(skills = List(500) { Skill("", "X".repeat(200), "advanced", "") })
            try { repository.analyze(cv, jobGapOffer(), false); fail("Se esperaba límite de cuerpo") }
            catch (error: JobSkillsGapException) { assertEquals(JobSkillsGapFailure.INVALID_INPUT, error.failure) }
            assertEquals(0, server.requestCount)
        } finally { server.shutdown() }
    }
}

private fun assertFailure(failure: JobSkillsGapFailure, block: () -> Unit) {
    try { block(); fail("Se esperaba un error explícito") }
    catch (error: JobSkillsGapException) { assertEquals(failure, error.failure) }
}
