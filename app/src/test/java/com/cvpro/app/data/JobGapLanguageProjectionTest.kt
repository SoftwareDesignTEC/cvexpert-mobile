package com.cvpro.app.data

import com.cvpro.app.data.mapper.toEvaluationRequest
import com.cvpro.app.data.mapper.toJobSkillsGapRequest
import com.cvpro.app.data.remote.api.JobApi
import com.cvpro.app.data.remote.api.SkillsGapApi
import com.cvpro.app.data.remote.dto.JobGapCvSkillDto
import com.cvpro.app.data.repository.ApiJobSkillsGapRepository
import com.cvpro.app.domain.model.JobSkillsGapException
import com.cvpro.app.domain.model.JobSkillsGapFailure
import com.cvpro.app.domain.model.Language
import com.cvpro.app.domain.model.Skill
import com.google.gson.Gson
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.lang.reflect.Proxy
import java.util.Locale

/** JVM-only fixtures. No device, server, socket, persistent update or real APIM call. */
class JobGapLanguageProjectionTest {
    private val gson = Gson()
    private fun language(name: String = "Inglés", level: String = "advanced") = Language("language1", name, level)
    private fun skill(name: String, level: String = "intermediate") = Skill("skill1", name, level, "technical")
    private fun cvWithEnglish() = jobGapCv(level = "intermediate").copy(languages = listOf(language()))

    @Test fun `advanced English documented under languages is sent under the exact offer name`() {
        val request = cvWithEnglish().toJobSkillsGapRequest(jobGapOffer(skills = listOf("Python", "English")))
        assertEquals(listOf(JobGapCvSkillDto("Python", "intermediate"), JobGapCvSkillDto("English", "advanced")), request.cv.skills)
    }

    @Test fun `Spanish spelling of English is retained when required by the offer`() {
        val request = cvWithEnglish().toEvaluationRequest(jobGapOffer(skills = listOf("Inglés")))
        assertEquals(JobGapCvSkillDto("Inglés", "advanced"), request.cv.skills.last())
    }

    @Test fun `case spaces accents BOM and NBSP use ECMAScript identity and preserve offer spelling`() {
        val cv = cvWithEnglish().copy(languages = listOf(language("\uFEFF\u00A0INgLÈS\u00A0")))
        val required = " \tEnGlIsH\n "
        assertEquals(JobGapCvSkillDto(required, "advanced"),
            cv.toJobSkillsGapRequest(jobGapOffer(skills = listOf(required))).cv.skills.last())
        val sign = "Costa\u2003Rican\u202FSign\u3000Language"
        val signCv = jobGapCv().copy(skills = emptyList(), languages = listOf(language("Lengua de señas costarricense")))
        assertEquals(JobGapCvSkillDto(sign, "advanced"), signCv.toEvaluationRequest(jobGapOffer(skills = listOf(sign))).cv.skills.single())
    }

    @Test fun `all twelve explicit alias pairs work in both directions`() {
        for ((spanish, english) in listOf(
            "Inglés" to "English", "Español" to "Spanish", "Francés" to "French",
            "Portugués" to "Portuguese", "Alemán" to "German", "Italiano" to "Italian",
            "Ruso" to "Russian", "Mandarín" to "Mandarin", "Japonés" to "Japanese",
            "Coreano" to "Korean", "Árabe" to "Arabic", "Lengua de señas costarricense" to "Costa Rican Sign Language",
        )) {
            for ((documented, required) in listOf(spanish to english, english to spanish)) {
                val cv = jobGapCv().copy(skills = emptyList(), languages = listOf(language(documented)))
                assertEquals(JobGapCvSkillDto(required, "advanced"),
                    cv.toJobSkillsGapRequest(jobGapOffer(skills = listOf(required))).cv.skills.single())
            }
        }
    }

    @Test fun `acute grave circumflex and diaeresis normalize but tilde and punctuation remain distinct`() {
        for (name in listOf("Inglés", "Inglès", "Inglês", "Inglës", "Ingle\u0301s")) {
            val cv = jobGapCv().copy(skills = emptyList(), languages = listOf(language(name)))
            assertEquals("English", cv.toEvaluationRequest(jobGapOffer(skills = listOf("English"))).cv.skills.single().name)
        }
        for ((documented, required) in listOf("Español" to "Espanol", "English" to "English literature",
            "Inglés" to "JavaScript", "English" to "English!", "English\u0085" to "English")) {
            val cv = jobGapCv().copy(skills = emptyList(), languages = listOf(language(documented)))
            assertTrue(cv.toEvaluationRequest(jobGapOffer(skills = listOf(required))).cv.skills.isEmpty())
        }
    }

    @Test fun `unknown language names match only the same normalized name without translation`() {
        val cv = jobGapCv().copy(skills = emptyList(), languages = listOf(language("Neerlandés")))
        assertEquals("NEERLANDES", cv.toEvaluationRequest(jobGapOffer(skills = listOf("NEERLANDES"))).cv.skills.single().name)
        assertTrue(cv.toEvaluationRequest(jobGapOffer(skills = listOf("Dutch"))).cv.skills.isEmpty())
    }

    @Test fun `identity is independent of the device locale`() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            val cv = jobGapCv().copy(skills = emptyList(), languages = listOf(language("INGLÉS")))
            assertEquals("English", cv.toEvaluationRequest(jobGapOffer(skills = listOf("English"))).cv.skills.single().name)
        } finally { Locale.setDefault(original) }
    }

    @Test fun `an undocumented language remains absent`() {
        val request = jobGapCv().toEvaluationRequest(jobGapOffer(skills = listOf("Python", "English")))
        assertEquals(listOf(JobGapCvSkillDto("Python", "advanced")), request.cv.skills)
    }

    @Test fun `a documented language not required by the offer is not added`() {
        val request = cvWithEnglish().toJobSkillsGapRequest(jobGapOffer(skills = listOf("Python")))
        assertEquals(listOf(JobGapCvSkillDto("Python", "intermediate")), request.cv.skills)
    }

    @Test fun `each valid linguistic level has an explicit operational mapping including native`() {
        for ((original, adapted) in listOf("basic" to "beginner", "intermediate" to "intermediate",
            "advanced" to "advanced", "native" to "expert")) {
            val cv = jobGapCv().copy(skills = emptyList(), languages = listOf(language(level = original)))
            assertEquals(JobGapCvSkillDto("English", adapted), cv.toEvaluationRequest(jobGapOffer(skills = listOf("English"))).cv.skills.single())
            assertEquals(original, cv.languages.single().level)
        }
    }

    @Test fun `invalid language levels are rejected without defaults even when not required`() {
        for (level in listOf("", "expert", "Native", "unknown", "advanced ")) {
            val cv = jobGapCv().copy(languages = listOf(language(level = level)))
            assertFailure(JobSkillsGapFailure.INVALID_CV_LEVEL) { cv.toJobSkillsGapRequest(jobGapOffer()) }
            assertFailure(JobSkillsGapFailure.INVALID_CV_LEVEL) { cv.toEvaluationRequest(jobGapOffer()) }
        }
        for (name in listOf("", " \t ", "\uFEFF\u00A0")) {
            assertFailure(JobSkillsGapFailure.INVALID_CV) {
                jobGapCv().copy(languages = listOf(language(name))).toEvaluationRequest(jobGapOffer())
            }
        }
    }

    @Test fun `skills take precedence over languages using the same name or an alias`() {
        val cv = jobGapCv().copy(skills = listOf(skill("Inglés", "beginner")), languages = listOf(language("English")))
        for (name in listOf("Inglés", "English")) {
            assertEquals(listOf(JobGapCvSkillDto(name, "beginner")), cv.toEvaluationRequest(jobGapOffer(skills = listOf(name))).cv.skills)
        }
    }

    @Test fun `first equivalent language or skill wins without selecting the highest level`() {
        val cv = jobGapCv().copy(skills = emptyList(), languages = listOf(language(level = "basic"), language("English", "native")))
        val offer = jobGapOffer(skills = listOf("English"))
        assertEquals("beginner", cv.toEvaluationRequest(offer).cv.skills.single().level)
        val declared = cv.copy(skills = listOf(skill("Inglés", "intermediate"), skill("English", "expert")))
        assertEquals("intermediate", declared.toEvaluationRequest(offer).cv.skills.single().level)
    }

    @Test fun `distinct offer aliases are both projected and normalized repeats are removed`() {
        val cv = cvWithEnglish().copy(skills = emptyList())
        val offer = jobGapOffer(skills = listOf("Inglés", "English", " INGLÉS ", "ENGLISH"))
        assertEquals(listOf(JobGapCvSkillDto("Inglés", "advanced"), JobGapCvSkillDto("English", "advanced")),
            cv.toJobSkillsGapRequest(offer).cv.skills)
    }

    @Test fun `technical skill names and levels remain unchanged including Java JavaScript C and C plus plus`() {
        val skills = listOf(skill("Java", "beginner"), skill("JavaScript", "expert"), skill("C", "intermediate"), skill("C++", "advanced"))
        val cv = jobGapCv().copy(skills = skills, languages = listOf(language()))
        val offer = jobGapOffer(skills = listOf("JavaScript", "C++", "English literature"))
        assertEquals(skills.map { JobGapCvSkillDto(it.name, it.level) }, cv.toEvaluationRequest(offer).cv.skills)
    }

    @Test fun `contradictory normalized technical skills fail explicitly and same levels deduplicate`() {
        val cv = jobGapCv().copy(skills = listOf(skill(" Java ", "beginner"), skill("JAVA", "advanced")))
        assertFailure(JobSkillsGapFailure.INVALID_CV) { cv.toJobSkillsGapRequest(jobGapOffer()) }
        val consistent = cv.copy(skills = listOf(skill(" Java ", "beginner"), skill("JAVA", "beginner")))
        assertEquals(listOf(JobGapCvSkillDto(" Java ", "beginner")), consistent.toEvaluationRequest(jobGapOffer()).cv.skills)
    }

    @Test fun `both endpoints use the same CV projection and preserve their different job contracts`() {
        val cv = cvWithEnglish()
        val offer = jobGapOffer(skills = listOf("Python", "English")).copy(description = "English required without a level.")
        val recommendations = cv.toJobSkillsGapRequest(offer)
        val evaluation = cv.toEvaluationRequest(offer)
        assertEquals(recommendations.cv, evaluation.cv)
        assertEquals(offer.skills, recommendations.jobs.single().skills)
        assertEquals(offer.skills, evaluation.jobs.single().skills)
        assertEquals(offer.description, evaluation.jobs.single().description)
        assertEquals(offer.requirements, evaluation.jobs.single().requirements)
        assertEquals(setOf("id", "skills"), gson.toJsonTree(recommendations).asJsonObject["jobs"].asJsonArray[0].asJsonObject.keySet())
        assertEquals(setOf("id", "skills", "description", "requirements"), gson.toJsonTree(evaluation).asJsonObject["jobs"].asJsonArray[0].asJsonObject.keySet())
    }

    @Test fun `original CV remains unchanged and neither JSON request contains personal fields or languages`() {
        val cv = cvWithEnglish()
        val original = gson.toJson(cv)
        val offer = jobGapOffer(skills = listOf("English"))
        for (request in listOf(cv.toJobSkillsGapRequest(offer), cv.toEvaluationRequest(offer))) {
            val json = gson.toJsonTree(request).asJsonObject
            assertEquals(setOf("cv", "jobs"), json.keySet())
            assertEquals(setOf("id", "skills"), json["cv"].asJsonObject.keySet())
            json["cv"].asJsonObject["skills"].asJsonArray.forEach {
                assertEquals(setOf("name", "level"), it.asJsonObject.keySet())
            }
            for (privateField in listOf("languages", "personalInfo", "userId", "email", "private-user", "Nombre privado"))
                assertFalse(gson.toJson(request).contains(privateField))
        }
        assertEquals(original, gson.toJson(cv))
        assertEquals("Inglés", cv.languages.single().name)
        assertEquals("advanced", cv.languages.single().level)
    }

    @Test fun `language only CV projects a required language while completely empty CV remains valid`() {
        val cv = cvWithEnglish().copy(skills = emptyList())
        val offer = jobGapOffer(skills = listOf("English"))
        assertEquals(listOf(JobGapCvSkillDto("English", "advanced")), cv.toEvaluationRequest(offer).cv.skills)
        val empty = cv.copy(languages = emptyList())
        assertTrue(empty.toEvaluationRequest(offer).cv.skills.isEmpty())
        assertTrue(empty.toJobSkillsGapRequest(offer).cv.skills.isEmpty())
    }

    @Test fun `final projected count includes languages and rejects overflow without truncating`() {
        val offer = jobGapOffer(skills = listOf("English"))
        val skills = List(500) { skill("Technology$it") }
        val accepted = cvWithEnglish().copy(skills = skills.take(499))
        assertEquals(500, accepted.toEvaluationRequest(offer).cv.skills.size)
        val excessive = accepted.copy(skills = skills)
        assertFailure(JobSkillsGapFailure.INVALID_CV) { excessive.toEvaluationRequest(offer) }
        assertFailure(JobSkillsGapFailure.INVALID_CV) { excessive.toJobSkillsGapRequest(offer) }
        assertEquals(500, excessive.skills.size)
        assertEquals(1, excessive.languages.size)
        val aliases = excessive.copy(skills = skills.take(499) + skill("English"), languages = emptyList())
        assertFailure(JobSkillsGapFailure.INVALID_CV) {
            aliases.toEvaluationRequest(jobGapOffer(skills = listOf("Inglés", "English")))
        }
    }

    @Test fun `offer identifiers and Unicode field limits remain enforced after adaptation`() {
        val cv = cvWithEnglish().copy(skills = emptyList())
        for (offer in listOf(jobGapOffer(skills = List(201) { "English" }), jobGapOffer(id = "x".repeat(201)),
            jobGapOffer(skills = listOf("English\u0000")), jobGapOffer(skills = listOf("x".repeat(201))))) {
            assertFailure(JobSkillsGapFailure.INVALID_INPUT) { cv.toEvaluationRequest(offer) }
        }
        val emoji = "\uD83D\uDE00"
        val unicode = cv.copy(languages = listOf(language(emoji.repeat(200))))
        assertEquals(emoji.repeat(200), unicode.toEvaluationRequest(jobGapOffer(skills = listOf(emoji.repeat(200)))).cv.skills.single().name)
        val invalid = cv.copy(skills = listOf(skill("\uD800")))
        assertFailure(JobSkillsGapFailure.INVALID_CV) { invalid.toEvaluationRequest(jobGapOffer()) }
    }

    @Test fun `100 KiB limit checks the final projected request before any API call`() = runTest {
        val cv = cvWithEnglish().copy(skills = List(499) { skill(it.toString().padStart(3, '0') + "X".repeat(197)) })
        val offer = jobGapOffer(skills = listOf("English"))
        val repository = ApiJobSkillsGapRepository(unexpectedApi<SkillsGapApi>(), unexpectedApi<JobApi>())
        for (evaluation in listOf(false, true)) {
            val request = if (evaluation) cv.toEvaluationRequest(offer) else cv.toJobSkillsGapRequest(offer)
            assertTrue(gson.toJson(request).toByteArray(Charsets.UTF_8).size > 100 * 1024)
            try {
                repository.analyze(cv, offer, evaluation)
                fail("Expected a body-size failure before any API call")
            } catch (error: JobSkillsGapException) {
                assertEquals(if (evaluation) JobSkillsGapFailure.TOO_LARGE else JobSkillsGapFailure.INVALID_INPUT, error.failure)
            }
        }
    }

    private inline fun <reified T> unexpectedApi(): T = Proxy.newProxyInstance(T::class.java.classLoader,
        arrayOf(T::class.java)) { _, method, _ -> error("Unexpected API invocation: ${method.name}") } as T

    private fun assertFailure(expected: JobSkillsGapFailure, block: () -> Unit) {
        try { block(); fail("Expected $expected") }
        catch (error: JobSkillsGapException) { assertEquals(expected, error.failure) }
    }
}
