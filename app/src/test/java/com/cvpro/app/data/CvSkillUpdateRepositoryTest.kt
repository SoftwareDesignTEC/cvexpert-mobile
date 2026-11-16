package com.cvpro.app.data

import com.cvpro.app.data.mapper.toUpdateRequest
import com.cvpro.app.data.remote.api.CvApi
import com.cvpro.app.data.repository.ApiCvRepository
import com.cvpro.app.domain.model.*
import com.cvpro.app.domain.repository.UnsafeCvUpdateException
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class CvSkillUpdateRepositoryTest {
    private val gson = Gson()
    private fun completeCv() = jobGapCv().copy(
        workExperiences = listOf(WorkExperience("w", "Company", "Position", 100, 200, false, "Description", "formal")),
        education = listOf(Education("e", "University", "Degree", "Field", 100, 200, false)),
        languages = listOf(Language("l", "Spanish", "native")),
        certifications = listOf(Certification("c", "Certificate", "Issuer", 100, 200, "https://example.org")))

    private fun rawCv(): JsonObject = gson.toJsonTree(completeCv().toUpdateRequest()).asJsonObject.apply {
        addProperty("id", "cv1"); addProperty("userId", "private-user")
        addProperty("createdAt", 100); addProperty("updatedAt", 200)
    }

    @Test fun existingGetAndPutPreserveEveryEditableSection() = runTest {
        val server = MockWebServer()
        server.start()
        try {
            val api = Retrofit.Builder().baseUrl(server.url("/"))
                .addConverterFactory(GsonConverterFactory.create()).build().create(CvApi::class.java)
            val repository = ApiCvRepository(api)
            val raw = rawCv()
            server.enqueue(MockResponse().setBody(raw.toString()))
            val latest = repository.getCvForSkillUpdate("cv1")
            val modified = latest.copy(skills = latest.skills + Skill("", "Git", "expert", ""))
            val confirmed = gson.toJsonTree(modified.toUpdateRequest()).asJsonObject.apply { addProperty("id", "cv1") }
            server.enqueue(MockResponse().setBody(confirmed.toString()))
            assertEquals("Git", repository.updateCv(modified).skills.last().name)
            assertEquals("/cvs/cv1", server.takeRequest().path)
            val request = server.takeRequest()
            assertEquals("PUT", request.method)
            assertEquals("/cvs/cv1", request.path)
            val body = gson.fromJson(request.body.readUtf8(), JsonObject::class.java)
            val expected = gson.toJsonTree(latest.toUpdateRequest()).asJsonObject
            body.remove("skills"); expected.remove("skills")
            assertEquals(expected, body)
        } finally { server.shutdown() }
    }

    @Test fun unknownFieldsAndLossyMappingsBlockBeforeAnyPut() = runTest {
        val variants = listOf<(JsonObject) -> Unit>(
            { it.addProperty("unmodeledSection", "must survive") },
            { it["personalInfo"].asJsonObject.addProperty("birthDate", "2000-01-01") },
            { it["personalInfo"].asJsonObject.add("modalities", gson.toJsonTree(listOf("unknown-mode"))) },
            { it["workExperiences"].asJsonArray[0].asJsonObject.addProperty("startDate", "unreadable date") },
            { it["certifications"].asJsonArray[0].asJsonObject.addProperty("extra", "must survive") },
            { it.addProperty("id", "another-cv") },
        )
        for (change in variants) {
            val server = MockWebServer()
            server.start()
            try {
                val api = Retrofit.Builder().baseUrl(server.url("/"))
                    .addConverterFactory(GsonConverterFactory.create()).build().create(CvApi::class.java)
                server.enqueue(MockResponse().setBody(rawCv().apply(change).toString()))
                try {
                    ApiCvRepository(api).getCvForSkillUpdate("cv1")
                    fail("Unsafe CV accepted")
                } catch (_: UnsafeCvUpdateException) { }
                assertEquals(1, server.requestCount)
                assertEquals("GET", server.takeRequest().method)
            } finally { server.shutdown() }
        }
    }
}
