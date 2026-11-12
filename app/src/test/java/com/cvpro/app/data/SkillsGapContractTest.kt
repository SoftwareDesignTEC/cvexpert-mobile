package com.cvpro.app.data

import com.cvpro.app.data.mapper.toDomain
import com.cvpro.app.data.remote.dto.SkillsGapPageDto
import com.cvpro.app.domain.model.SkillsGapException
import com.cvpro.app.domain.model.SkillsGapFailure
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SkillsGapContractTest {
    @Test
    fun `maps ordinal levels and preserves unknown and zero duration`() {
        val page = parsePage(completePage)
        assertNull(page.items!!.single().courses!!.first().durationHours)
        val analysis = page.toDomain()
        val gap = analysis.skills.single()
        assertEquals(1, gap.userLevel)
        assertEquals(4, gap.marketLevel)
        assertEquals(3, gap.deficitSteps)
        assertNull(analysis.recommendations[0].duration)
        assertEquals("0 horas", analysis.recommendations[1].duration)
        assertEquals("cv-skill-v1", analysis.scaleKey)
    }

    @Test
    fun `preserves incomplete analysis and unresolved skills with nullable levels`() {
        val analysis = parsePage(pageJson(
            status = "incomplete",
            unresolved = """
                [{"skillName":"Example A","reason":"MISSING_USER_SKILL","userLevel":null,"marketLevel":4},
                 {"skillName":"Example B","reason":"UNKNOWN_LEVEL","userLevel":null,"marketLevel":null}]
            """.trimIndent(),
        )).toDomain()

        assertTrue(analysis.skills.isEmpty())
        assertEquals("incomplete", analysis.status)
        assertEquals("available", analysis.marketStatus)
        assertEquals(2, analysis.unresolved.size)
        assertEquals("MISSING_USER_SKILL", analysis.unresolved.first().reason)
        assertNull(analysis.unresolved.first().userLevel)
        assertEquals(4, analysis.unresolved.first().marketLevel)
        assertFalse(analysis.canConfirmNoGaps)
    }

    @Test
    fun `empty results require full coverage and zero total to confirm no gaps`() {
        val complete = parsePage(pageJson()).toDomain()
        assertTrue(complete.canConfirmNoGaps)
        assertFalse(parsePage(pageJson(marketStatus = "missing")).toDomain().canConfirmNoGaps)
        assertFalse(parsePage(pageJson(status = "empty", comparedSkills = 0)).toDomain().canConfirmNoGaps)
        assertFalse(parsePage(pageJson(page = 2, total = 1)).toDomain().canConfirmNoGaps)
        assertFalse(parsePage(pageJson(comparedSkills = 0)).toDomain().canConfirmNoGaps)
        assertFalse(complete.copy(unresolved = listOf(
            com.cvpro.app.domain.model.UnresolvedSkill("Example", "SCALE_MISMATCH", 1, 3),
        )).canConfirmNoGaps)
    }

    @Test
    fun `rejects percentages inconsistent deficits and missing 020 metadata`() {
        listOf(
            completePage.replace("\"userLevel\":1", "\"userLevel\":40"),
            completePage.replace("\"deficitSteps\":3", "\"deficitSteps\":2"),
            completePage.replace("\"deficitSteps\":3,", ""),
            """{"items":[],"page":1,"pageSize":100,"totalRecords":0}""",
            pageJson().replace("cv-skill-v1", "unknown-scale"),
        ).forEach { json ->
            val failure = assertThrows(SkillsGapException::class.java) { parsePage(json).toDomain() }
            assertEquals(SkillsGapFailure.INCOMPATIBLE_RESPONSE, failure.failure)
        }
    }

    private fun parsePage(json: String) = Gson().fromJson(json, SkillsGapPageDto::class.java)
}

internal fun pageJson(
    status: String = "complete",
    marketStatus: String = "available",
    comparedSkills: Int = 1,
    unresolved: String = "[]",
    page: Long = 1,
    total: Long = 0,
    items: String = "[]",
): String = """
    {"items":$items,"page":$page,"pageSize":100,"totalRecords":$total,
     "analysis":{"status":"$status","comparedSkills":$comparedSkills,"scaleKey":"cv-skill-v1",
                 "marketStatus":"$marketStatus","unresolved":$unresolved}}
""".trimIndent()

internal val completePage = pageJson(total = 1, items = """
    [{"skillName":"Example","userLevel":1,"marketLevel":4,"deficitSteps":3,
      "courses":[
        {"id":"1","title":"Course A","url":"https://example.com/a","platform":"Provider","logoUrl":null,"durationHours":null},
        {"id":"2","title":"Course B","url":"https://example.com/b","platform":"Provider","logoUrl":null,"durationHours":0}
      ]}]
""".trimIndent())
