package com.cvpro.app.domain

import com.cvpro.app.domain.model.JobModality
import com.cvpro.app.domain.model.JobOffer
import com.cvpro.app.domain.model.JobPreferences
import com.cvpro.app.domain.model.rankedFor
import org.junit.Assert.assertEquals
import org.junit.Test

class JobRankingTest {

    private fun offer(id: String, modality: JobModality, type: String, match: Int?) = JobOffer(
        id = id,
        title = id,
        company = "",
        companyLogoUrl = null,
        location = "",
        modality = modality,
        sector = "",
        type = type,
        description = "",
        requirements = emptyList(),
        salary = null,
        matchScore = match,
        postedAt = "",
    )

    private val remotePartTime = offer("remoto-medio", JobModality.REMOTE, "part-time", 40)
    private val remoteFullTime = offer("remoto-completo", JobModality.REMOTE, "full-time", 60)
    private val onSiteFullTime = offer("presencial", JobModality.ON_SITE, "full-time", 95)

    @Test
    fun withoutPreferencesOrdersByMatchOnly() {
        val ranked = listOf(remotePartTime, remoteFullTime, onSiteFullTime).rankedFor(JobPreferences.NONE)
        assertEquals(listOf("presencial", "remoto-completo", "remoto-medio"), ranked.map { it.id })
    }

    @Test
    fun preferencesWinOverMatchScore() {
        val prefs = JobPreferences(modalities = setOf(JobModality.REMOTE))
        val ranked = listOf(onSiteFullTime, remotePartTime, remoteFullTime).rankedFor(prefs)
        // Las dos remotas primero (entre ellas por compatibilidad) aunque la presencial tenga 95.
        assertEquals(listOf("remoto-completo", "remoto-medio", "presencial"), ranked.map { it.id })
    }

    @Test
    fun meetingBothPreferencesComesFirst() {
        val prefs = JobPreferences(types = setOf("part-time"), modalities = setOf(JobModality.REMOTE))
        val ranked = listOf(onSiteFullTime, remoteFullTime, remotePartTime).rankedFor(prefs)
        assertEquals(listOf("remoto-medio", "remoto-completo", "presencial"), ranked.map { it.id })
    }

    @Test
    fun emptyListDoesNotCount() {
        val prefs = JobPreferences(types = setOf("full-time"))
        assertEquals(0, prefs.affinity(remotePartTime))
        assertEquals(1, prefs.affinity(onSiteFullTime))
    }
}
