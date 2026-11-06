package com.cvpro.app.skills

import com.cvpro.app.data.remote.api.SkillsGapApi
import com.cvpro.app.data.repository.ApiSkillsGapRepository
import com.cvpro.app.domain.model.*
import com.google.gson.JsonParser
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class SkillsGapRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var api: SkillsGapApi
    @Before fun setup() {
        server = MockWebServer()
        server.start()
        api = Retrofit.Builder().baseUrl(server.url("/gateway/"))
            .addConverterFactory(GsonConverterFactory.create()).build().create(SkillsGapApi::class.java)
    }
    @After fun teardown() { server.shutdown() }

    @Test fun disabledRepositoryMakesNoRequests() = runTest {
        val repo = ApiSkillsGapRepository(api, false)
        try { repo.getAnalysis(); fail("must reject") }
        catch (e: SkillsGapException) { assertEquals(SkillsGapFailure.DISABLED, e.failure) }
        try { repo.replaceInput(emptyList()); fail("must reject") }
        catch (e: SkillsGapException) { assertEquals(SkillsGapFailure.DISABLED, e.failure) }
        assertEquals(0, server.requestCount)
    }

    @Test fun putSendsOnlySkillsAndExplicitNullThenGetUsesGatewayPagination() = runTest {
        val repo = ApiSkillsGapRepository(api, true)
        server.enqueue(MockResponse().setResponseCode(204))
        server.enqueue(MockResponse().setHeader("X-Data-Mode", "reference").setBody(completePage))
        repo.replaceInput(listOf(SkillInput("SQL", "beginner"), SkillInput("Python", null)))
        val put = server.takeRequest()
        assertEquals("PUT", put.method)
        assertEquals("/gateway/skills-gap/input", put.path)
        val json = JsonParser.parseString(put.body.readUtf8()).asJsonObject
        assertEquals(setOf("skills"), json.keySet())
        val skills = json.getAsJsonArray("skills")
        assertEquals(setOf("skillName", "level"), skills[0].asJsonObject.keySet())
        assertTrue(skills[1].asJsonObject["level"].isJsonNull)
        listOf("X-User-Id", "X-User-Email", "X-ARR-ClientCert").forEach { assertNull(put.getHeader(it)) }
        assertEquals(3, repo.getAnalysis().skills.single().deficitSteps)
        assertEquals("/gateway/skills-gap?page=1&pageSize=100", server.takeRequest().path)
    }

    @Test fun httpFailuresAreTypedForBothOperations() = runTest {
        val repo = ApiSkillsGapRepository(api, true)
        for ((code, failure) in mapOf(401 to SkillsGapFailure.UNAUTHORIZED,
            409 to SkillsGapFailure.INPUT_REQUIRED, 503 to SkillsGapFailure.UNAVAILABLE,
            400 to SkillsGapFailure.INVALID_INPUT, 413 to SkillsGapFailure.INVALID_INPUT,
            403 to SkillsGapFailure.UNAVAILABLE, 404 to SkillsGapFailure.UNAVAILABLE)) {
            repeat(2) { operation ->
                server.enqueue(MockResponse().setResponseCode(code).setBody("{\"error\":\"private details\"}"))
                try {
                    if (operation == 0) repo.getAnalysis() else repo.replaceInput(emptyList())
                    fail("must reject HTTP $code")
                } catch (e: SkillsGapException) { assertEquals(failure, e.failure) }
            }
        }
    }

    @Test fun mockResponsesAndUnexpectedPutSuccessAreRejected() = runTest {
        val repo = ApiSkillsGapRepository(api, true)
        for (header in listOf("demo", "mock", "")) {
            server.enqueue(MockResponse().setHeader("X-Data-Mode", header).setBody(completePage))
            try { repo.getAnalysis(); fail("must reject mock") }
            catch (e: SkillsGapException) { assertEquals(SkillsGapFailure.INCOMPATIBLE_RESPONSE, e.failure) }
        }
        server.enqueue(MockResponse().setResponseCode(200))
        try { repo.replaceInput(emptyList()); fail("PUT requires 204") }
        catch (e: SkillsGapException) { assertEquals(SkillsGapFailure.INCOMPATIBLE_RESPONSE, e.failure) }
    }

    @Test fun oversizedUtf8SnapshotFailsBeforeSending() = runTest {
        val repo = ApiSkillsGapRepository(api, true)
        // Under both the 500-skill and 200-UTF16-unit limits, over 100 KiB on the wire.
        val skills = List(200) { SkillInput("$it" + "界".repeat(190), "expert") }
        try { repo.replaceInput(skills); fail("must reject oversized body") }
        catch (e: SkillsGapException) { assertEquals(SkillsGapFailure.INVALID_INPUT, e.failure) }
        assertEquals(0, server.requestCount)
    }
}
