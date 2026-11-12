package com.cvpro.app.data

import com.cvpro.app.data.mapper.*
import com.cvpro.app.data.remote.api.*
import com.cvpro.app.data.remote.AuthInterceptor
import com.cvpro.app.domain.repository.AuthRepository
import com.cvpro.app.data.remote.dto.*
import com.cvpro.app.data.repository.ApiJobGapEvaluationRepository
import com.cvpro.app.domain.model.*
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.MutableStateFlow
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import java.lang.reflect.Proxy

class JobGapEvaluationTest {
    private val gson = Gson()

    @Test fun `real CV and offer retain canonical levels and both text fields without personal data`() {
        val cv = evaluationCv()
        val offer = evaluationOffer()
        val request = cv.toEvaluationRequest(offer)
        assertEquals(listOf("intermediate", "advanced"), request.cv.skills.map { it.level })
        assertEquals(offer.description, request.jobs.single().description)
        assertEquals(offer.requirements, request.jobs.single().requirements)
        val json = gson.toJsonTree(request).asJsonObject
        assertEquals(setOf("id", "skills"), json["cv"].asJsonObject.keySet())
        assertEquals(setOf("id", "skills", "description", "requirements"), json["jobs"].asJsonArray[0].asJsonObject.keySet())
        assertEquals(1, request.jobs.size)
    }

    @Test fun `backend deficits meets and absent skills are preserved without local comparison`() {
        val result = evaluationResult()
        val (python, sql, docker) = result.assessments!!
        assertEquals(SkillLevel.INTERMEDIATE, python.cvLevel)
        assertEquals(SkillLevel.ADVANCED, python.requiredLevel)
        assertEquals(1, python.deficitSteps)
        assertEquals("documented_skill_below_required", python.status)
        assertEquals(SkillLevel.ADVANCED, sql.cvLevel)
        assertEquals(SkillLevel.BEGINNER, sql.requiredLevel)
        assertEquals(0, sql.deficitSteps)
        assertEquals("documented_skill_meets_required", sql.status)
        assertNull(docker.cvLevel)
        assertNull(docker.deficitSteps)
        assertEquals("Python avanzado", python.evidenceText)
        assertEquals(1, result.levelAnalysis!!.below)
    }

    @Test fun `unknown optional and conflicting requirements stay unknown`() {
        for ((detection, reason) in listOf("unknown" to "no_explicit_evidence", "unknown" to "optional",
            "conflict" to "contradictory", "conflict" to "ambiguous", "conflict" to "negated")) {
            val dto = gson.fromJson(evaluationJson, JobGapEvaluationResponseDto::class.java)
            val result = dto.results!!.single()
            val assessment = result.skillLevelAssessments!!.first().copy(requiredLevel = null,
                evidenceSource = null, evidenceText = null, detectionStatus = detection, reason = reason,
                status = "unknown_required_level", deficitSteps = null)
            val actual = dto.copy(results = listOf(result.copy(skillLevelAssessments =
                listOf(assessment) + result.skillLevelAssessments.drop(1)))).toEvaluation("cv1", "job1").assessments!!.first()
            assertNull(actual.requiredLevel)
            assertNull(actual.deficitSteps)
            assertEquals(reason, actual.reason)
        }
    }

    @Test fun `legacy responses have no level analysis and complete coverage can coexist with deficits`() {
        val dto = gson.fromJson(evaluationJson, JobGapEvaluationResponseDto::class.java)
        val result = dto.results!!.single()
        val legacy = dto.copy(results = listOf(result.copy(skillLevelAssessments = null, levelAnalysis = null)))
            .toEvaluation("cv1", "job1")
        assertNull(legacy.assessments)
        assertNull(legacy.levelAnalysis)
        val complete = dto.copy(results = listOf(result.copy(status = "complete", requiredSkillsCount = 2,
            documentedCoveragePercent = 100.0, notDocumentedSkills = emptyList(),
            skillLevelAssessments = result.skillLevelAssessments!!.take(2),
            levelAnalysis = result.levelAnalysis!!.copy(notDocumentedCount = 0))))
            .toEvaluation("cv1", "job1")
        assertEquals(100.0, complete.documentedCoveragePercent!!, 0.0)
        assertEquals(1, complete.assessments!!.first().deficitSteps)
    }

    @Test fun `empty CV is valid but any invalid skill blocks the entire request`() {
        assertTrue(evaluationCv().copy(skills = emptyList()).toEvaluationRequest(evaluationOffer()).cv.skills.isEmpty())
        for (level in listOf("", "unknown", "Advanced", "básico")) {
            assertFailure(JobGapFailure.INVALID_CV) {
                evaluationCv().copy(skills = listOf(Skill("", "Python", level, "")))
                    .toEvaluationRequest(evaluationOffer())
            }
        }
        for (name in listOf("", "  ", "\uFEFF", "\u00A0")) assertFailure(JobGapFailure.INVALID_CV) {
            evaluationCv().copy(skills = listOf(Skill("", name, "beginner", ""))).toEvaluationRequest(evaluationOffer())
        }
    }

    @Test fun `all field limits use Unicode code points without truncation`() {
        val cv = evaluationCv()
        val offer = evaluationOffer()
        val emoji = "\uD83D\uDE00"
        val accepted = offer.copy(description = emoji.repeat(20_000), requirements = List(100) { emoji.repeat(2_000) },
            skills = List(200) { "Python" })
        assertEquals(40_000, cv.toEvaluationRequest(accepted).jobs.single().description!!.length)
        for (invalid in listOf(offer.copy(description = "a".repeat(20_001)),
            offer.copy(requirements = List(101) { "a" }), offer.copy(requirements = listOf("a".repeat(2_001))),
            offer.copy(skills = List(201) { "a" }), offer.copy(description = "bad\u0000text"),
            offer.copy(description = "isolated\uD800"), offer.copy(id = "bad\nid"))) {
            assertFailure(JobGapFailure.INVALID_INPUT) { cv.toEvaluationRequest(invalid) }
        }
        assertFailure(JobGapFailure.INVALID_CV) {
            cv.copy(skills = List(501) { cv.skills.first() }).toEvaluationRequest(offer)
        }
        assertFailure(JobGapFailure.INVALID_CV) {
            cv.copy(skills = listOf(cv.skills.first().copy(name = "a".repeat(201)))).toEvaluationRequest(offer)
        }
    }

    @Test fun `HTTP evaluation uses gateway prefix no reference header and no identity headers`() = runTest {
        withServer { server, repository ->
            server.enqueue(MockResponse().setBody(evaluationJson))
            repository.evaluate(evaluationCv(), evaluationOffer())
            val request = server.takeRequest(5, TimeUnit.SECONDS)!!
            assertEquals("POST", request.method)
            assertEquals("/gateway/skills-gap/evaluate", request.path)
            listOf("X-User-Id", "X-User-Email", "X-ARR-ClientCert", "X-Data-Mode").forEach {
                assertNull(request.getHeader(it))
            }
            assertEquals("Python avanzado y SQL básico.", JsonParser.parseString(request.body.readUtf8())
                .asJsonObject["jobs"].asJsonArray[0].asJsonObject["description"].asString)
            assertEquals(1, server.requestCount)
        }
    }

    @Test fun `existing AuthInterceptor attaches bearer token to the new endpoint`() = runTest {
        val server = MockWebServer()
        server.start()
        try {
            // A non-JWT fixture bypasses only the local expiry parser; no real credential is used.
            val auth = Proxy.newProxyInstance(AuthRepository::class.java.classLoader,
                arrayOf(AuthRepository::class.java)) { _, method, _ ->
                if (method.name == "getIdToken") MutableStateFlow("unit-test-token") else error("Unexpected auth call")
            } as AuthRepository
            val retrofit = Retrofit.Builder().baseUrl(server.url("/gateway/"))
                .client(OkHttpClient.Builder().addInterceptor(AuthInterceptor(auth)).build())
                .addConverterFactory(GsonConverterFactory.create()).build()
            server.enqueue(MockResponse().setBody(evaluationJson))
            ApiJobGapEvaluationRepository(retrofit.create(SkillsGapApi::class.java), retrofit.create(JobApi::class.java))
                .evaluate(evaluationCv(), evaluationOffer())
            val request = server.takeRequest(5, TimeUnit.SECONDS)!!
            assertEquals("Bearer unit-test-token", request.getHeader("Authorization"))
            assertEquals("android", request.getHeader("X-Client"))
            assertNull(request.getHeader("X-User-Id"))
            assertNull(request.getHeader("X-User-Email"))
        } finally { server.shutdown() }
    }

    @Test fun `missing list fields trigger job detail before exactly one evaluation`() = runTest {
        withServer { server, repository ->
            server.enqueue(MockResponse().setBody(gson.toJson(JobOfferDto(id = "job1",
                skills = listOf("Python", "SQL", "Docker"), description = evaluationOffer().description,
                requirements = evaluationOffer().requirements))))
            server.enqueue(MockResponse().setBody(evaluationJson))
            repository.evaluate(evaluationCv(), evaluationOffer().copy(description = "", requirements = emptyList(), skills = emptyList()))
            assertEquals("/gateway/jobs/job1", server.takeRequest(5, TimeUnit.SECONDS)!!.path)
            val post = server.takeRequest(5, TimeUnit.SECONDS)!!
            assertEquals("/gateway/skills-gap/evaluate", post.path)
            assertTrue(post.body.readUtf8().contains("Docker intermedio"))
            assertEquals(2, server.requestCount)
        }
    }

    @Test fun `job without skills returns insufficient information and null coverage`() = runTest {
        withServer { server, repository ->
            server.enqueue(MockResponse().setBody("""{"id":"job1","skills":[],"description":"General","requirements":[]}"""))
            server.enqueue(MockResponse().setBody("""{"cvId":"cv1","dataSource":"client_request","results":[{"jobId":"job1","status":"insufficient_information","requiredSkillsCount":0,"documentedMatchesCount":0,"documentedCoveragePercent":null,"matchedSkills":[],"notDocumentedSkills":[]}]}"""))
            val actual = repository.evaluate(evaluationCv(), evaluationOffer().copy(skills = emptyList()))
            assertNull(actual.documentedCoveragePercent)
            assertEquals("insufficient_information", actual.status)
            assertEquals(0, actual.requiredSkillsCount)
        }
    }

    @Test fun `HTTP failures never expose backend bodies`() = runTest {
        withServer { server, repository ->
            for ((code, expected) in listOf(400 to JobGapFailure.INVALID_INPUT, 401 to JobGapFailure.UNAUTHORIZED,
                403 to JobGapFailure.FORBIDDEN, 404 to JobGapFailure.NOT_FOUND, 413 to JobGapFailure.TOO_LARGE,
                503 to JobGapFailure.UNAVAILABLE)) {
                server.enqueue(MockResponse().setResponseCode(code).setBody("private backend details"))
                try { repository.evaluate(evaluationCv(), evaluationOffer()); fail("Expected failure") }
                catch (error: JobGapException) {
                    assertEquals(expected, error.failure)
                    assertFalse(error.message!!.contains("private"))
                }
            }
        }
    }

    @Test fun `oversize JSON is rejected before POST even when individual fields fit`() = runTest {
        withServer { server, repository ->
            val offer = evaluationOffer().copy(requirements = List(100) { "á".repeat(1_000) })
            try { repository.evaluate(evaluationCv(), offer); fail("Expected failure") }
            catch (error: JobGapException) { assertEquals(JobGapFailure.TOO_LARGE, error.failure) }
            assertEquals(0, server.requestCount)
        }
    }

    @Test fun `mismatched and incomplete responses are rejected`() {
        val dto = gson.fromJson(evaluationJson, JobGapEvaluationResponseDto::class.java)
        for (invalid in listOf(dto.copy(cvId = "other"), dto.copy(results = emptyList()),
            dto.copy(results = listOf(dto.results!!.single().copy(jobId = "other"))),
            dto.copy(results = listOf(dto.results.single().copy(matchedSkills = null))))) {
            assertFailure(JobGapFailure.INCOMPATIBLE) { invalid.toEvaluation("cv1", "job1") }
        }
    }

    private suspend fun withServer(block: suspend (MockWebServer, ApiJobGapEvaluationRepository) -> Unit) {
        val server = MockWebServer()
        server.start()
        try {
            val retrofit = Retrofit.Builder().baseUrl(server.url("/gateway/"))
                .addConverterFactory(GsonConverterFactory.create()).build()
            block(server, ApiJobGapEvaluationRepository(retrofit.create(SkillsGapApi::class.java), retrofit.create(JobApi::class.java)))
        } finally { server.shutdown() }
    }

    private fun assertFailure(expected: JobGapFailure, block: () -> Unit) {
        try { block(); fail("Expected failure") }
        catch (error: JobGapException) { assertEquals(expected, error.failure) }
    }
}

internal fun evaluationCv(): Cv = CvDto(id = "cv1", title = "CV de desarrollo",
    skills = listOf(SkillDto(name = "Python", level = "intermediate"), SkillDto(name = "SQL", level = "advanced"))).toDomain()

internal fun evaluationOffer(): JobOffer = JobOfferDto(id = "job1", title = "Desarrollador",
    skills = listOf("Python", "SQL", "Docker"), description = "Python avanzado y SQL básico.",
    requirements = listOf("Docker intermedio")).toDomain()

internal fun evaluationResult(): JobGapEvaluation = Gson().fromJson(evaluationJson, JobGapEvaluationResponseDto::class.java)
    .toEvaluation("cv1", "job1")

internal val evaluationJson = """
{"cvId":"cv1","dataSource":"client_request","results":[{
 "jobId":"job1","status":"partial","requiredSkillsCount":3,"documentedMatchesCount":2,
 "documentedCoveragePercent":66.67,"matchedSkills":[{"name":"Python","cvLevel":"intermediate"},{"name":"SQL","cvLevel":"advanced"}],
 "notDocumentedSkills":["Docker"],"skillLevelAssessments":[
 {"name":"Python","cvLevel":"intermediate","requiredLevel":"advanced","evidenceSource":"description","evidenceText":"Python avanzado","detectionStatus":"explicit","reason":null,"status":"documented_skill_below_required","deficitSteps":1},
 {"name":"SQL","cvLevel":"advanced","requiredLevel":"beginner","evidenceSource":"description","evidenceText":"SQL básico","detectionStatus":"explicit","reason":null,"status":"documented_skill_meets_required","deficitSteps":0},
 {"name":"Docker","cvLevel":null,"requiredLevel":"intermediate","evidenceSource":"requirements","evidenceText":"Docker intermedio","detectionStatus":"explicit","reason":null,"status":"not_documented","deficitSteps":null}],
 "levelAnalysis":{"scale":"cv-skill-v1","comparedSkillsCount":2,"belowRequiredCount":1,"meetsRequiredCount":1,"notDocumentedCount":1,"unknownRequiredLevelCount":0,"conflictCount":0}
}]}
""".trimIndent()
