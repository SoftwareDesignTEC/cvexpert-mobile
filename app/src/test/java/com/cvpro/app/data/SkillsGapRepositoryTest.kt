package com.cvpro.app.data

import com.cvpro.app.data.remote.api.SkillsGapApi
import com.cvpro.app.data.repository.ApiSkillsGapRepository
import com.cvpro.app.domain.model.SkillInput
import com.cvpro.app.domain.model.SkillLevel
import com.cvpro.app.domain.model.SkillsGapException
import com.cvpro.app.domain.model.SkillsGapFailure
import com.google.gson.JsonParser
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class SkillsGapRepositoryTest {
    private val server = MockWebServer()
    private lateinit var repository: ApiSkillsGapRepository

    @Before
    fun setup() {
        server.start()
        val api = Retrofit.Builder()
            .baseUrl(server.url("/gateway/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SkillsGapApi::class.java)
        repository = ApiSkillsGapRepository(api)
    }

    @After
    fun teardown() {
        server.shutdown()
    }

    @Test
    fun `puts the exact input shape with all level values and explicit null`() = runTest {
        val levels = SkillLevel.entries + null
        val skills = levels.mapIndexed { index, level -> SkillInput("Skill $index", level) }
        server.enqueue(MockResponse().setResponseCode(204))
        repository.replaceInput(skills)

        val request = checkNotNull(server.takeRequest(5, TimeUnit.SECONDS))
        assertEquals("PUT", request.method)
        assertEquals("/gateway/skills-gap/input", request.path)
        assertTrue(request.getHeader("Content-Type")!!.startsWith("application/json"))
        val body = JsonParser.parseString(request.body.readUtf8()).asJsonObject
        assertEquals(setOf("skills"), body.keySet())
        val wireSkills = body.getAsJsonArray("skills").map { it.asJsonObject }
        assertEquals(skills.map { it.skillName }, wireSkills.map { it["skillName"].asString })
        wireSkills.forEach { assertEquals(setOf("skillName", "level"), it.keySet()) }
        assertEquals(
            listOf("beginner", "intermediate", "advanced", "expert"),
            wireSkills.take(4).map { it["level"].asString },
        )
        assertTrue(wireSkills.last()["level"].isJsonNull)
        listOf("X-User-Id", "X-User-Email", "X-ARR-ClientCert").forEach { assertNull(request.getHeader(it)) }
    }

    @Test
    fun `gets the requested page under the existing gateway prefix`() = runTest {
        server.enqueue(MockResponse().setHeader("X-Data-Mode", "reference").setBody(pageJson(page = 2, total = 101)))
        val analysis = repository.getAnalysis(page = 2)
        val request = checkNotNull(server.takeRequest(5, TimeUnit.SECONDS))
        assertEquals("GET", request.method)
        assertEquals("/gateway/skills-gap?page=2&pageSize=100", request.path)
        assertEquals(2L, analysis.page)
        assertEquals(101L, analysis.totalRecords)
        assertTrue(analysis.skills.isEmpty())
    }

    @Test
    fun `only the documented GET conflict requests skills input`() = runTest {
        val required = """{"error":"SKILLS_INPUT_REQUIRED","message":"Enter skills"}"""
        server.enqueue(MockResponse().setResponseCode(409).setBody(required))
        expectFailure(SkillsGapFailure.INPUT_REQUIRED) { repository.getAnalysis() }

        for (body in listOf("""{"error":"OTHER"}""", "<html>Gateway error</html>", "")) {
            server.enqueue(MockResponse().setResponseCode(409).setBody(body))
            expectFailure(SkillsGapFailure.UNAVAILABLE) { repository.getAnalysis() }
        }

        server.enqueue(MockResponse().setResponseCode(409).setBody(required))
        expectFailure(SkillsGapFailure.UNAVAILABLE) { repository.replaceInput(validSkills) }
    }

    @Test
    fun `rejects gateway mocks and requires 204 after writing`() = runTest {
        server.enqueue(MockResponse().setHeader("X-Data-Mode", "mock").setBody(completePage))
        expectFailure(SkillsGapFailure.INCOMPATIBLE_RESPONSE) { repository.getAnalysis() }

        server.enqueue(MockResponse().setResponseCode(200))
        expectFailure(SkillsGapFailure.INCOMPATIBLE_RESPONSE) { repository.replaceInput(validSkills) }
    }

    @Test
    fun `rejects empty invalid and oversized snapshots before sending`() = runTest {
        listOf(
            emptyList(),
            listOf(SkillInput(" ", null)),
            listOf(SkillInput("name\u0000", null)),
            listOf(SkillInput(" name ", null)),
            listOf(SkillInput("a".repeat(201), null)),
            validSkills + validSkills,
            List(501) { SkillInput("Skill $it", null) },
            List(200) { SkillInput("$it" + "界".repeat(190), SkillLevel.EXPERT) },
        ).forEach { invalidSkills ->
            expectFailure(SkillsGapFailure.INVALID_INPUT) { repository.replaceInput(invalidSkills) }
        }
        assertEquals(0, server.requestCount)
    }

    private suspend fun expectFailure(expected: SkillsGapFailure, operation: suspend () -> Unit) {
        try {
            operation()
            throw AssertionError("Expected $expected")
        } catch (error: SkillsGapException) {
            assertEquals(expected, error.failure)
        }
    }

    private val validSkills = listOf(SkillInput("Example", SkillLevel.BEGINNER))
}
