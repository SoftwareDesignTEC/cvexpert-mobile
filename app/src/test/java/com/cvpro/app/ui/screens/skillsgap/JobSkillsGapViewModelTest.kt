package com.cvpro.app.ui.screens.skillsgap

import com.cvpro.app.R
import com.cvpro.app.data.jobGapCv
import com.cvpro.app.data.jobGapOffer
import com.cvpro.app.data.jobGapResponse
import com.cvpro.app.data.mapper.toJobAnalysis
import com.cvpro.app.data.mapper.toJobSkillsGapRequest
import com.cvpro.app.data.remote.dto.JobSkillsGapResponseDto
import com.cvpro.app.domain.model.*
import com.cvpro.app.domain.repository.CvRepository
import com.cvpro.app.domain.repository.JobSkillsGapRepository
import com.google.gson.Gson
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class JobSkillsGapViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }

    @Test fun unifiedFlowKeepsCoursesAndAddsBackendLevels() = runTest(dispatcher) {
        val levels = com.cvpro.app.data.evaluationResult()
        val courses = levels.copy(assessments = null, levelAnalysis = null,
            catalogStatus = JobCatalogStatus.CONSULTED,
            recommendations = listOf(JobSkillCourses("Docker", SkillCatalogMatch.NOT_FOUND, emptyList())))
        val gaps = JobGapRepository().apply { result = courses; evaluationResult = levels }
        val vm = JobSkillsGapViewModel(JobGapCvs(), gaps, com.cvpro.app.data.testJobOffers(), EmptyCourseRepository())
        vm.open(jobGapOffer()); advanceUntilIdle()
        vm.selectCv("cv1"); advanceUntilIdle()
        vm.analyze(); vm.analyze(); advanceUntilIdle()
        assertEquals(listOf(false, true), gaps.modes)
        assertEquals(courses.recommendations, vm.uiState.value.analysis!!.recommendations)
        assertEquals(1, vm.uiState.value.analysis!!.assessments!!.first().deficitSteps)
        assertEquals(1, vm.uiState.value.analysis!!.levelAnalysis!!.below)
        assertNull(vm.uiState.value.errorMessage)
    }

    @Test fun evaluationOutagePreservesAlreadyAvailableCourses() = runTest(dispatcher) {
        val courses = Gson().fromJson(jobGapResponse, JobSkillsGapResponseDto::class.java).toJobAnalysis("cv1", "job1", false)
        val gaps = JobGapRepository().apply { result = courses; evaluationFailure = JobSkillsGapFailure.UNAVAILABLE }
        val vm = JobSkillsGapViewModel(JobGapCvs(), gaps, com.cvpro.app.data.testJobOffers(), EmptyCourseRepository())
        vm.open(jobGapOffer()); advanceUntilIdle()
        vm.selectCv("cv1"); advanceUntilIdle()
        vm.analyze(); advanceUntilIdle()
        assertEquals(courses.recommendations, vm.uiState.value.analysis!!.recommendations)
        assertNull(vm.uiState.value.analysis!!.levelAnalysis)
        assertEquals(R.string.job_gap_unavailable, vm.uiState.value.errorMessage)
    }

    @Test fun catalogueOutageStillAllowsLevelEvaluationWithoutCourses() = runTest(dispatcher) {
        val levels = com.cvpro.app.data.evaluationResult()
        val gaps = JobGapRepository().apply { failure = JobSkillsGapFailure.CATALOG_UNAVAILABLE; evaluationResult = levels }
        val vm = JobSkillsGapViewModel(JobGapCvs(), gaps, com.cvpro.app.data.testJobOffers(), EmptyCourseRepository())
        vm.open(jobGapOffer()); advanceUntilIdle()
        vm.selectCv("cv1"); advanceUntilIdle()
        vm.analyze(); advanceUntilIdle()
        assertTrue(vm.uiState.value.canEvaluateOnly)
        gaps.failure = null
        vm.analyze(evaluationOnly = true); advanceUntilIdle()
        assertEquals(levels.assessments, vm.uiState.value.analysis!!.assessments)
        assertTrue(vm.uiState.value.analysis!!.recommendations.isEmpty())
        assertNull(vm.uiState.value.analysis!!.catalogStatus)
    }

    @Test fun selectedCvIsLoadedByIdAndItsActualSkillsReachTheRepository() = runTest(dispatcher) {
        val cvs = JobGapCvs()
        val gaps = JobGapRepository()
        val vm = JobSkillsGapViewModel(cvs, gaps, com.cvpro.app.data.testJobOffers(), EmptyCourseRepository())
        vm.open(jobGapOffer())
        assertTrue(vm.uiState.value.isLoading)
        advanceUntilIdle()
        vm.selectCv("cv1")
        advanceUntilIdle()
        assertEquals(listOf("cv1"), cvs.detailCalls)
        vm.analyze()
        advanceUntilIdle()
        assertEquals("advanced", gaps.receivedCv!!.skills.single().level)
        assertEquals("job1", gaps.receivedOffer!!.id)
        assertEquals("cv1", vm.uiState.value.analysis!!.cvId)
        assertFalse(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.errorMessage)
    }

    @Test fun failedCvDetailDoesNotBecomeAnEmptyCvAndCanBeRetried() = runTest(dispatcher) {
        val cvs = JobGapCvs().apply { detailError = IOException() }
        val gaps = JobGapRepository()
        val vm = JobSkillsGapViewModel(cvs, gaps, com.cvpro.app.data.testJobOffers(), EmptyCourseRepository())
        vm.open(jobGapOffer()); advanceUntilIdle()
        vm.selectCv("cv1"); advanceUntilIdle()
        vm.analyze(); advanceUntilIdle()
        assertNull(vm.uiState.value.selectedCv)
        assertNull(vm.uiState.value.analysis)
        assertEquals(0, gaps.calls)
        assertEquals(R.string.job_gap_network_error, vm.uiState.value.errorMessage)
        cvs.detailError = null
        vm.selectCv("cv1"); advanceUntilIdle()
        assertNotNull(vm.uiState.value.selectedCv)
    }

    @Test fun invalidLevelAllowsCorrectionWithoutInventingARequestLevel() = runTest(dispatcher) {
        val cvs = JobGapCvs().apply { detail = jobGapCv(level = "") }
        val vm = JobSkillsGapViewModel(cvs, JobGapRepository(), com.cvpro.app.data.testJobOffers(), EmptyCourseRepository())
        vm.open(jobGapOffer()); advanceUntilIdle()
        vm.selectCv("cv1"); advanceUntilIdle()
        vm.analyze(); advanceUntilIdle()
        assertTrue(vm.uiState.value.needsCvCorrection)
        assertEquals(R.string.job_gap_invalid_level, vm.uiState.value.errorMessage)
        assertNull(vm.uiState.value.analysis)
    }

    @Test fun catalogOutageOffersExplicitPureEvaluationAndAvailabilityErrorsRemainErrors() = runTest(dispatcher) {
        val gaps = JobGapRepository().apply { failure = JobSkillsGapFailure.CATALOG_UNAVAILABLE }
        val vm = JobSkillsGapViewModel(JobGapCvs(), gaps, com.cvpro.app.data.testJobOffers(), EmptyCourseRepository())
        vm.open(jobGapOffer()); advanceUntilIdle()
        vm.selectCv("cv1"); advanceUntilIdle()
        vm.analyze(); advanceUntilIdle()
        assertTrue(vm.uiState.value.canEvaluateOnly)
        assertNull(vm.uiState.value.analysis)
        gaps.failure = null
        vm.analyze(true); advanceUntilIdle()
        assertEquals(listOf(false, true), gaps.modes)
        assertNull(vm.uiState.value.analysis!!.catalogStatus)
        assertTrue(vm.uiState.value.analysis!!.recommendations.isEmpty())
        gaps.failure = JobSkillsGapFailure.UNAVAILABLE
        vm.analyze(); advanceUntilIdle()
        assertEquals(R.string.job_gap_unavailable, vm.uiState.value.errorMessage)
        assertFalse(vm.uiState.value.canEvaluateOnly)
        assertNull(vm.uiState.value.analysis)
    }

    @Test fun changingSelectionCancelsPendingDetailAndClearsPreviousResults() = runTest(dispatcher) {
        val cvs = JobGapCvs()
        val vm = JobSkillsGapViewModel(cvs, JobGapRepository(), com.cvpro.app.data.testJobOffers(), EmptyCourseRepository())
        vm.open(jobGapOffer()); advanceUntilIdle()
        val delayed = CompletableDeferred<Cv>()
        cvs.pending = delayed
        vm.selectCv("cv1"); runCurrent()
        assertTrue(vm.uiState.value.isLoading)
        cvs.pending = null
        cvs.detail = jobGapCv("cv2")
        vm.selectCv("cv2"); advanceUntilIdle()
        delayed.complete(jobGapCv("cv1")); advanceUntilIdle()
        assertEquals("cv2", vm.uiState.value.selectedCv!!.id)
        assertNull(vm.uiState.value.analysis)
        vm.close()
        assertNull(vm.uiState.value.offer)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test fun emptyAndFailedCvListsAreDifferentAndPaginationIsPreserved() = runTest(dispatcher) {
        val cvs = JobGapCvs().apply { listError = IOException() }
        val vm = JobSkillsGapViewModel(cvs, JobGapRepository(), com.cvpro.app.data.testJobOffers(), EmptyCourseRepository())
        vm.open(jobGapOffer()); advanceUntilIdle()
        assertEquals(0, vm.uiState.value.cvPage)
        assertNotNull(vm.uiState.value.errorMessage)
        cvs.listError = null
        cvs.items = emptyList()
        vm.loadCvs(); advanceUntilIdle()
        assertEquals(1, vm.uiState.value.cvPage)
        assertTrue(vm.uiState.value.cvs.isEmpty())
        assertNull(vm.uiState.value.errorMessage)
    }

    @Test fun completeInsufficientAndNoCourseStatesRemainSuccessfulDistinctResults() = runTest(dispatcher) {
        val gaps = JobGapRepository()
        val vm = JobSkillsGapViewModel(JobGapCvs(), gaps, com.cvpro.app.data.testJobOffers(), EmptyCourseRepository())
        vm.open(jobGapOffer()); advanceUntilIdle()
        vm.selectCv("cv1"); advanceUntilIdle()
        val partial = Gson().fromJson(jobGapResponse, JobSkillsGapResponseDto::class.java)
            .toJobAnalysis("cv1", "job1", false)
        val states = listOf(
            partial.copy(recommendations = listOf(partial.recommendations.single().copy(courses = emptyList()))),
            partial.copy(status = JobCoverageStatus.COMPLETE, requiredSkillsCount = 1,
                documentedCoveragePercent = 100.0, notDocumentedSkills = emptyList(),
                catalogStatus = JobCatalogStatus.NOT_NEEDED, recommendations = emptyList()),
            partial.copy(status = JobCoverageStatus.INSUFFICIENT_INFORMATION, requiredSkillsCount = 0,
                documentedMatchesCount = 0, documentedCoveragePercent = null, matchedSkills = emptyList(),
                notDocumentedSkills = emptyList(), catalogStatus = JobCatalogStatus.NOT_NEEDED, recommendations = emptyList()),
        )
        for (result in states) {
            gaps.result = result
            vm.analyze(); advanceUntilIdle()
            assertEquals(result, vm.uiState.value.analysis)
            assertNull(vm.uiState.value.errorMessage)
            assertFalse(vm.uiState.value.canEvaluateOnly)
        }
    }
    @Test fun levelCoursesAreLazyCachedDeduplicatedAndPagedByExactSkill() = runTest(dispatcher) {
        val courses = RecordingCourses()
        val gaps = JobGapRepository().apply { evaluationResult = com.cvpro.app.data.evaluationResult(); result = evaluationResult!!.copy(assessments = null, levelAnalysis = null) }
        val vm = JobSkillsGapViewModel(JobGapCvs(), gaps, com.cvpro.app.data.testJobOffers(), courses)
        vm.open(jobGapOffer()); advanceUntilIdle(); vm.selectCv("cv1"); advanceUntilIdle(); vm.analyze(); advanceUntilIdle()
        assertTrue(courses.calls.isEmpty())
        val skill = vm.uiState.value.analysis!!.assessments!!.first { it.canSuggestLevelCourses() }.name
        vm.loadLevelCourses(skill); vm.loadLevelCourses(skill); advanceUntilIdle()
        vm.loadLevelCourses(skill); advanceUntilIdle()
        assertEquals(listOf(Triple(1, 5, skill)), courses.calls)
        assertEquals(1, vm.uiState.value.levelCourses[skill]!!.items.size)
        vm.loadLevelCourses(skill, true); advanceUntilIdle()
        assertEquals(2, courses.calls.size)
        assertEquals(2, vm.uiState.value.levelCourses[skill]!!.page)
        assertEquals(1, vm.uiState.value.levelCourses[skill]!!.items.size)
        assertEquals(6L, vm.uiState.value.levelCourses[skill]!!.totalRecords)
    }
    @Test fun courseFailuresPreserveEvaluationAndMissingSkillRecommendationsAndRetry() = runTest(dispatcher) {
        val courses = RecordingCourses().apply { failure = CourseException(CourseFailure.CONNECTION) }
        val gaps = JobGapRepository().apply { evaluationResult = com.cvpro.app.data.evaluationResult(); result = evaluationResult!!.copy(assessments = null, levelAnalysis = null) }
        val vm = JobSkillsGapViewModel(JobGapCvs(), gaps, com.cvpro.app.data.testJobOffers(), courses)
        vm.open(jobGapOffer()); advanceUntilIdle(); vm.selectCv("cv1"); advanceUntilIdle(); vm.analyze(); advanceUntilIdle()
        val original = vm.uiState.value.analysis!!
        val skill = original.assessments!!.first { it.canSuggestLevelCourses() }.name
        vm.loadLevelCourses(skill); advanceUntilIdle()
        assertEquals(original, vm.uiState.value.analysis)
        assertEquals(CourseFailure.CONNECTION, vm.uiState.value.levelCourses[skill]!!.error)
        courses.failure = null
        vm.loadLevelCourses(skill); advanceUntilIdle()
        assertNull(vm.uiState.value.levelCourses[skill]!!.error)
        assertEquals(listOf(1, 1), courses.calls.map { it.first })
        assertEquals(original.recommendations, vm.uiState.value.analysis!!.recommendations)
    }
    @Test fun changingCvOrOfferIgnoresNonCooperativeOldCourseResponse() = runTest(dispatcher) {
        for (changeOffer in listOf(false, true)) {
            val courses = RecordingCourses()
            val gaps = JobGapRepository().apply { evaluationResult = com.cvpro.app.data.evaluationResult(); result = evaluationResult!!.copy(assessments = null, levelAnalysis = null) }
            val vm = JobSkillsGapViewModel(JobGapCvs(), gaps, object : com.cvpro.app.domain.repository.JobOfferRepository {
                override suspend fun getOffers(page: Int, pageSize: Int, query: String) = PagedResult(listOf(jobGapOffer(), jobGapOffer("job2")), page, pageSize, 2)
            }, courses)
            vm.start(); advanceUntilIdle(); vm.selectCv("cv1"); advanceUntilIdle(); vm.selectOffer("job1"); vm.analyze(); advanceUntilIdle()
            val skill = vm.uiState.value.analysis!!.assessments!!.first { it.canSuggestLevelCourses() }.name
            val pending = CompletableDeferred<CoursePage>()
            courses.pending = pending
            vm.loadLevelCourses(skill); runCurrent()
            if (changeOffer) vm.selectOffer("job2") else vm.selectCv("cv2")
            runCurrent()
            pending.complete(CoursePage(emptyList(), 1, 5, 0)); advanceUntilIdle()
            assertTrue(vm.uiState.value.levelCourses.isEmpty())
            assertNull(vm.uiState.value.analysis)
        }
    }
    @Test fun sufficientUnknownConflictingAndMissingSkillsNeverQueryLevelCatalog() = runTest(dispatcher) {
        val courses = RecordingCourses()
        val base = com.cvpro.app.data.evaluationResult()
        val assessment = base.assessments!!.first()
        for (item in listOf(assessment.copy(status = "documented_skill_meets_required", deficitSteps = 0),
            assessment.copy(detectionStatus = "unknown", requiredLevel = null), assessment.copy(detectionStatus = "conflict", requiredLevel = null, reason = "contradictory"),
            assessment.copy(status = "not_documented", cvLevel = null))) {
            val gaps = JobGapRepository().apply { evaluationResult = base.copy(assessments = listOf(item)); result = base.copy(assessments = null, levelAnalysis = null) }
            val vm = JobSkillsGapViewModel(JobGapCvs(), gaps, com.cvpro.app.data.testJobOffers(), courses)
            vm.open(jobGapOffer()); advanceUntilIdle(); vm.selectCv("cv1"); advanceUntilIdle(); vm.analyze(); advanceUntilIdle()
            vm.loadLevelCourses(item.name); advanceUntilIdle()
        }
        assertTrue(courses.calls.isEmpty())
    }
    @Test fun levelCatalogConcurrencyIsBoundedAndQueuedRequestsCancelOnClose() = runTest(dispatcher) {
        val courses = RecordingCourses().apply { pending = CompletableDeferred() }
        val base = com.cvpro.app.data.evaluationResult()
        val assessment = base.assessments!!.first { it.canSuggestLevelCourses() }
        val names = listOf("Python", "SQL", "Docker", "Git")
        val gaps = JobGapRepository().apply { evaluationResult = base.copy(assessments = names.map { assessment.copy(name = it) }); result = base.copy(assessments = null, levelAnalysis = null) }
        val vm = JobSkillsGapViewModel(JobGapCvs(), gaps, com.cvpro.app.data.testJobOffers(), courses)
        vm.open(jobGapOffer()); advanceUntilIdle(); vm.selectCv("cv1"); advanceUntilIdle(); vm.analyze(); advanceUntilIdle()
        names.forEach { vm.loadLevelCourses(it) }; runCurrent()
        assertEquals(2, courses.calls.size)
        vm.close(); runCurrent()
        courses.pending!!.complete(CoursePage(emptyList(), 1, 5, 0)); advanceUntilIdle()
        assertTrue(vm.uiState.value.levelCourses.isEmpty())
        assertEquals(2, courses.calls.size)
    }

}

private class JobGapCvs : CvRepository {
    var detail = jobGapCv()
    var items = listOf(jobGapCv(), jobGapCv("cv2"))
    var detailError: Exception? = null
    var listError: Exception? = null
    var pending: CompletableDeferred<Cv>? = null
    val detailCalls = mutableListOf<String>()
    override suspend fun getCvs(page: Int, pageSize: Int, query: String): PagedResult<Cv> {
        listError?.let { throw it }
        return PagedResult(items, page, pageSize, items.size)
    }
    override suspend fun getCvById(id: String): Cv {
        detailCalls += id
        detailError?.let { throw it }
        return pending?.await() ?: detail
    }
    override suspend fun updateCv(cv: Cv): Cv = error("unused")
    override suspend fun createCv(title: String, templateId: String, personalInfo: UserProfile?): Cv = error("unused")
    override suspend fun deleteCv(id: String) = error("unused")
}

private class JobGapRepository : JobSkillsGapRepository {
    var calls = 0
    var receivedCv: Cv? = null
    var receivedOffer: JobOffer? = null
    var failure: JobSkillsGapFailure? = null
    var result: JobSkillsGapAnalysis? = null
    var evaluationResult: JobSkillsGapAnalysis? = null
    var evaluationFailure: JobSkillsGapFailure? = null
    val modes = mutableListOf<Boolean>()
    override suspend fun analyze(cv: Cv, offer: JobOffer, evaluationOnly: Boolean): JobSkillsGapAnalysis {
        calls++
        modes += evaluationOnly
        cv.toJobSkillsGapRequest(offer)
        receivedCv = cv
        receivedOffer = offer
        failure?.let { throw JobSkillsGapException(it) }
        if (evaluationOnly) {
            evaluationFailure?.let { throw JobSkillsGapException(it) }
            evaluationResult?.let { return it }
        }
        result?.let { return it }
        return Gson().fromJson(jobGapResponse, JobSkillsGapResponseDto::class.java)
            .toJobAnalysis(cv.id, offer.id, evaluationOnly)
    }
}

private class RecordingCourses : com.cvpro.app.domain.repository.CourseRepository {
    val calls = mutableListOf<Triple<Int, Int, String?>>()
    var failure: Exception? = null
    var pending: CompletableDeferred<CoursePage>? = null
    override suspend fun getCourses(page: Int, pageSize: Int, filters: CourseFilters): CoursePage {
        calls += Triple(page, pageSize, filters.skillName)
        failure?.let { throw it }
        // Simulate a transport which returns after its caller has been cancelled.
        pending?.let { return kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) { it.await() } }
        val course = Course("1", "Python", "Microsoft Learn", "https://learn.microsoft.com/training/", null, null, false, listOf("Python"))
        return CoursePage(listOf(course), page, pageSize, 6)
    }
}
