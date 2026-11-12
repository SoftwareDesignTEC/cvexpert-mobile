package com.cvpro.app.ui.screens.skillsgap

import com.cvpro.app.R
import com.cvpro.app.data.jobGapCv
import com.cvpro.app.data.jobGapOffer
import com.cvpro.app.domain.model.*
import com.cvpro.app.domain.repository.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class JobGapUxTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }

    @Test fun `2 of 7 means names documented and SQL meets while Excel remains unspecified`() {
        val result = assistantResult()
        val presentation = JobGapPresentation(result)
        assertEquals(7, result.requiredSkillsCount)
        assertEquals(2, result.documentedMatchesCount)
        assertEquals(28.57, result.documentedCoveragePercent!!, 0.0)
        assertEquals(listOf("SQL"), presentation.meets.map { it.name })
        assertTrue(presentation.below.isEmpty())
        assertEquals(listOf("Excel"), presentation.unspecified.map { it.name })
        assertNull(presentation.unspecified.single().requiredLevel)
        assertEquals(5, presentation.missing.size)
        assertTrue(result.assessments!!.filter { it.name in presentation.missing }.all { it.cvLevel == null && it.deficitSteps == null })
    }

    @Test fun `chart positions are ordinal and legacy responses never invent required levels`() {
        assertEquals(listOf(1, 2, 3, 4), SkillLevel.entries.map { it.ordinalPosition() })
        val presentation = JobGapPresentation(assistantResult().copy(assessments = null, levelAnalysis = null))
        assertEquals(2, presentation.unspecified.size)
        assertTrue(presentation.meets.isEmpty())
        assertTrue(presentation.documented.all { it.requiredLevel == null && it.cvLevel == SkillLevel.INTERMEDIATE })
    }

    @Test fun `empty courses are one group and shared course is displayed once`() {
        val result = assistantResult()
        assertEquals(result.notDocumentedSkills, JobGapPresentation(result).withoutCourses)
        val course = CourseRecommendation("c1", "Curso", "Plataforma", "https://example.org/course", null, null, false)
        val withCourses = result.copy(recommendations = listOf(
            JobSkillCourses("Office", SkillCatalogMatch.MATCHED, listOf(course)),
            JobSkillCourses("Soporte técnico", SkillCatalogMatch.MATCHED, listOf(course))))
        assertEquals(listOf(course), JobGapPresentation(withCourses).courses)
        assertTrue(JobGapPresentation(withCourses).withoutCourses.isEmpty())
    }

    @Test fun `main flow fetches repository offers only after explicit CV selection and submits chosen models`() = runTest(dispatcher) {
        val offers = UxOffers()
        val gaps = UxGaps()
        val vm = JobSkillsGapViewModel(UxCvs(), gaps, offers)
        vm.start(); vm.start(); advanceUntilIdle()
        assertNull(vm.uiState.value.selectedCv)
        assertNull(vm.uiState.value.offer)
        assertTrue(offers.calls.isEmpty())
        vm.selectCv("cv1"); advanceUntilIdle()
        assertEquals(listOf(1 to ""), offers.calls)
        assertEquals(offers.items.take(10), vm.uiState.value.offers)
        vm.selectOffer("job3")
        vm.analyze(); vm.analyze(); advanceUntilIdle()
        assertEquals("job3", gaps.offer!!.id)
        assertEquals("cv1", gaps.cv!!.id)
        assertEquals(listOf(false, true), gaps.calls)
        assertNotNull(vm.uiState.value.analysis)
    }

    @Test fun `Jobs entry uses preselected offer and does not fetch offer list`() = runTest(dispatcher) {
        val offers = UxOffers()
        val vm = JobSkillsGapViewModel(UxCvs(), UxGaps(), offers)
        vm.open(jobGapOffer()); advanceUntilIdle()
        vm.selectCv("cv1"); advanceUntilIdle()
        assertTrue(vm.uiState.value.offerLocked)
        vm.changeOfferQuery("different"); vm.loadOffers(); advanceUntilIdle()
        assertTrue(offers.calls.isEmpty())
        assertEquals("job1", vm.uiState.value.offer!!.id)
    }

    @Test fun `search is debounced paginated and discards late old query`() = runTest(dispatcher) {
        val offers = UxOffers()
        val vm = JobSkillsGapViewModel(UxCvs(), UxGaps(), offers)
        vm.start(); advanceUntilIdle(); vm.selectCv("cv1"); advanceUntilIdle()
        vm.loadOffers(); vm.loadOffers(); advanceUntilIdle()
        assertEquals(listOf(1 to "", 2 to ""), offers.calls)
        assertEquals(11, vm.uiState.value.offers.size)
        vm.changeOfferQuery("o"); vm.changeOfferQuery("old")
        offers.old = CompletableDeferred()
        advanceTimeBy(350); runCurrent()
        vm.changeOfferQuery("new"); advanceTimeBy(350); advanceUntilIdle()
        val actual = vm.uiState.value.offers
        offers.old!!.complete(PagedResult(listOf(jobGapOffer("stale")), 1, 10, 1)); advanceUntilIdle()
        assertEquals("new", vm.uiState.value.offerQuery)
        assertEquals(actual, vm.uiState.value.offers)
        assertFalse(offers.calls.any { it.second == "o" })
        assertFalse(vm.uiState.value.isLoadingOffers)
    }

    @Test fun `network error requires manual retry and keeps CV and job`() = runTest(dispatcher) {
        val gaps = UxGaps().apply { courseError = IOException("private") }
        val vm = selected(gaps)
        vm.analyze(); advanceUntilIdle()
        assertEquals(listOf(false), gaps.calls)
        assertEquals("cv1", vm.uiState.value.selectedCv!!.id)
        assertEquals("job1", vm.uiState.value.offer!!.id)
        assertEquals(R.string.job_gap_network_error, vm.uiState.value.errorMessage)
        gaps.courseError = null
        vm.retry(); vm.retry(); advanceUntilIdle()
        assertEquals(listOf(false, false, true), gaps.calls)
        assertNotNull(vm.uiState.value.analysis)
    }

    @Test fun `retry after evaluation failure does not repost successful recommendations`() = runTest(dispatcher) {
        val gaps = UxGaps().apply { levelError = IOException() }
        val vm = selected(gaps)
        vm.analyze(); advanceUntilIdle()
        val courses = vm.uiState.value.analysis!!.recommendations
        assertTrue(courses.isNotEmpty())
        assertNull(vm.uiState.value.analysis!!.assessments)
        gaps.levelError = null
        vm.retry(); advanceUntilIdle()
        assertEquals(listOf(false, true, true), gaps.calls)
        assertEquals(courses, vm.uiState.value.analysis!!.recommendations)
        assertNotNull(vm.uiState.value.analysis!!.assessments)
    }

    @Test fun `catalogue failure still allows explicit independent evaluation`() = runTest(dispatcher) {
        val gaps = UxGaps().apply { courseError = JobSkillsGapException(JobSkillsGapFailure.CATALOG_UNAVAILABLE) }
        val vm = selected(gaps)
        vm.analyze(); advanceUntilIdle()
        assertTrue(vm.uiState.value.canEvaluateOnly)
        vm.analyze(true); advanceUntilIdle()
        assertEquals(listOf(false, true), gaps.calls)
        assertNotNull(vm.uiState.value.analysis!!.assessments)
        assertTrue(vm.uiState.value.analysis!!.recommendations.isEmpty())
    }

    @Test fun `changing offer cancels analysis and stale response cannot replace new selection`() = runTest(dispatcher) {
        val gaps = UxGaps().apply { pending = CompletableDeferred() }
        val vm = JobSkillsGapViewModel(UxCvs(), gaps, UxOffers())
        vm.start(); advanceUntilIdle(); vm.selectCv("cv1"); advanceUntilIdle()
        vm.selectOffer("job1"); vm.analyze(); runCurrent()
        vm.selectOffer("job2")
        gaps.pending!!.complete(assistantResult()); advanceUntilIdle()
        assertEquals("job2", vm.uiState.value.offer!!.id)
        assertNull(vm.uiState.value.analysis)
        assertFalse(vm.uiState.value.isLoading)
    }

    private suspend fun TestScope.selected(gaps: UxGaps): JobSkillsGapViewModel {
        val vm = JobSkillsGapViewModel(UxCvs(), gaps, UxOffers())
        vm.open(jobGapOffer()); testScheduler.advanceUntilIdle()
        vm.selectCv("cv1"); testScheduler.advanceUntilIdle()
        return vm
    }
}

private class UxOffers : JobOfferRepository {
    val items = List(11) { jobGapOffer("job$it").copy(title = "Puesto $it", company = "Empresa $it") }
    val calls = mutableListOf<Pair<Int, String>>()
    var old: CompletableDeferred<PagedResult<JobOffer>>? = null
    override suspend fun getOffers(page: Int, pageSize: Int, query: String): PagedResult<JobOffer> {
        calls += page to query
        if (query == "old") return withContext(NonCancellable) { old!!.await() }
        return PagedResult(items.drop((page - 1) * pageSize).take(pageSize), page, pageSize, items.size)
    }
}

private class UxCvs : CvRepository {
    override suspend fun getCvs(page: Int, pageSize: Int, query: String) = PagedResult(listOf(jobGapCv()), page, pageSize, 1)
    override suspend fun getCvById(id: String) = jobGapCv(id)
    override suspend fun updateCv(cv: Cv): Cv = error("unused")
    override suspend fun createCv(title: String, templateId: String, personalInfo: UserProfile?): Cv = error("unused")
    override suspend fun deleteCv(id: String) = error("unused")
}

private class UxGaps : JobSkillsGapRepository {
    val calls = mutableListOf<Boolean>()
    var cv: Cv? = null
    var offer: JobOffer? = null
    var courseError: Exception? = null
    var levelError: Exception? = null
    var pending: CompletableDeferred<JobSkillsGapAnalysis>? = null
    override suspend fun analyze(cv: Cv, offer: JobOffer, evaluationOnly: Boolean): JobSkillsGapAnalysis {
        calls += evaluationOnly; this.cv = cv; this.offer = offer
        (if (evaluationOnly) levelError else courseError)?.let { throw it }
        pending?.let { return withContext(NonCancellable) { it.await() } }
        val result = assistantResult().copy(cvId = cv.id, jobId = offer.id)
        return if (evaluationOnly) result.copy(catalogStatus = null, recommendations = emptyList())
            else result.copy(assessments = null, levelAnalysis = null)
    }
}

private fun assistantResult(): JobSkillsGapAnalysis {
    val missing = listOf("Comunicación", "Logística", "Office", "Soporte técnico", "Trabajo en equipo")
    return JobSkillsGapAnalysis("cv1", "job1", JobCoverageStatus.PARTIAL, 7, 2, 28.57,
        listOf(DocumentedJobSkill("SQL", "intermediate"), DocumentedJobSkill("Excel", "intermediate")), missing,
        JobCatalogStatus.CONSULTED, missing.map { JobSkillCourses(it, SkillCatalogMatch.NOT_FOUND, emptyList()) },
        listOf(JobLevelAssessment("SQL", SkillLevel.INTERMEDIATE, SkillLevel.BEGINNER, "description", "SQL básico", "explicit", null, "documented_skill_meets_required", 0),
            JobLevelAssessment("Excel", SkillLevel.INTERMEDIATE, null, null, null, "unknown", "no_explicit_evidence", "unknown_required_level", null)) +
            missing.map { JobLevelAssessment(it, null, null, null, null, "unknown", "no_explicit_evidence", "not_documented", null) },
        JobLevelAnalysis(1, 0, 1, 5, 1, 0))
}
