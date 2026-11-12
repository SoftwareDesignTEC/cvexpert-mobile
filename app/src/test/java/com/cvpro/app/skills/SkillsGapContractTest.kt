package com.cvpro.app.skills

import com.cvpro.app.data.mapper.*
import com.cvpro.app.data.remote.dto.SkillsGapPageDto
import com.cvpro.app.domain.model.*
import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test

internal val completePage = """{
  "items":[{"skillName":"SQL","userLevel":1,"marketLevel":4,"deficitSteps":3,
    "courses":[{"id":"2","title":"SQL course","platform":"Catalog","url":"https://example.org/course",
    "logoUrl":null,"durationHours":null,"isPaid":false,"skills":["SQL"]}]}],
  "page":1,"pageSize":100,"totalRecords":1,
  "analysis":{"status":"complete","comparedSkills":2,"scaleKey":"cv-skill-v1",
    "marketStatus":"available","unresolved":[]}
}"""

internal fun parsePage(json: String) = Gson().fromJson(json, SkillsGapPageDto::class.java).toDomain()

class SkillsGapContractTest {
    @Test fun ordinalLevelsDeficitAndNullDurationSurviveDeserialization() {
        val result = parsePage(completePage)
        assertEquals(SkillGapItem("SQL", 1, 4, 3), result.skills.single())
        assertNull(result.recommendations.single().duration)
        assertEquals(2, result.comparedSkills)
        assertFalse(result.canConfirmNoGaps)
        assertEquals("0 horas", parsePage(completePage.replace("\"durationHours\":null", "\"durationHours\":0"))
            .recommendations.single().duration)
    }

    @Test fun missingMarketAndUnknownLevelsRemainUnresolved() {
        val result = parsePage("""{"items":[],"page":1,"pageSize":100,"totalRecords":0,
            "analysis":{"status":"incomplete","comparedSkills":0,"scaleKey":"cv-skill-v1",
            "marketStatus":"missing","unresolved":[{"skillName":"SQL","reason":"UNKNOWN_LEVEL",
            "userLevel":null,"marketLevel":null}]}}""")
        assertFalse(result.canConfirmNoGaps)
        assertNull(result.unresolved.single().userLevel)
        assertEquals("UNKNOWN_LEVEL", result.unresolved.single().reason)
        assertFalse(result.copy(status = "empty", unresolved = emptyList()).canConfirmNoGaps)
        assertFalse(result.copy(status = "complete", marketStatus = "available").canConfirmNoGaps)
        assertTrue(result.copy(status = "complete", marketStatus = "available", comparedSkills = 1,
            unresolved = emptyList()).canConfirmNoGaps)
    }

    @Test fun legacyAndInvalidOrdinalResponsesFailClosed() {
        listOf(
            """{"items":[],"page":1,"pageSize":100,"totalRecords":0}""",
            completePage.replace("\"userLevel\":1", "\"userLevel\":40"),
            completePage.replace("\"deficitSteps\":3,", ""),
            completePage.replace("cv-skill-v1", "unknown-scale"),
            completePage.replace("\"deficitSteps\":3", "\"deficitSteps\":2"),
        ).forEach { json ->
            assertEquals(SkillsGapFailure.INCOMPATIBLE_RESPONSE,
                assertThrows(SkillsGapException::class.java) { parsePage(json) }.failure)
        }
    }

    @Test fun cvMappingPreservesNamesAndMapsUnknownLevelsToExplicitNull() {
        val skills = listOf("beginner", "intermediate", "advanced", "expert", "", "NEW", "80")
            .mapIndexed { index, level -> Skill("$index", "Skill $index", level, "") }
        val input = skills.toSkillsGapInput()
        assertEquals(listOf("beginner", "intermediate", "advanced", "expert", null, null, null), input.map { it.level })
        val json = input.toInputJson()
        assertEquals(setOf("skills"), json.keySet())
        assertEquals(setOf("skillName", "level"), json.getAsJsonArray("skills")[4].asJsonObject.keySet())
        assertTrue(json.getAsJsonArray("skills")[4].asJsonObject["level"].isJsonNull)
    }

    @Test fun invalidSnapshotsAreRejectedWithoutRewritingNames() {
        listOf(
            listOf(SkillInput(" SQL", "beginner")),
            listOf(SkillInput("SQL", "expert"), SkillInput("SQL", "expert")),
            listOf(SkillInput("SQL", "unknown")),
            List(501) { SkillInput("$it", null) },
        ).forEach { input -> assertThrows(SkillsGapException::class.java) { input.toInputJson() } }
        assertEquals("{\"skills\":[]}", emptyList<SkillInput>().toInputJson().toString())
    }

    @Test fun paginationDoesNotImplyNoGapsForEmptyPage() {
        val result = SkillsGapAnalysis(emptyList(), emptyList(), status = "complete",
            comparedSkills = 10, marketStatus = "available", page = 2, totalRecords = 1)
        assertFalse(result.canConfirmNoGaps)
        assertFalse(result.hasMore)
        assertTrue(result.copy(page = 1, totalRecords = 101).hasMore)
    }
}
