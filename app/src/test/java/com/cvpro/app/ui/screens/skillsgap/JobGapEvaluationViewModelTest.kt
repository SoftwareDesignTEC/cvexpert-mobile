package com.cvpro.app.ui.screens.skillsgap

import com.cvpro.app.R
import com.cvpro.app.data.*
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
class JobGapEvaluationViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }

    @Test fun `selection is explicit and detail rather than list CV reaches evaluation`() = runTest(dispatcher) {
        val cvs = EvaluationCvs()
        val evaluations = EvaluationRepository()
        val vm = JobSkillsGapViewModel(cvs, evaluations)
        vm.open(evaluationOffer()); advanceUntilIdle()
        assertNull(vm.uiState.value.selectedCvId)
        vm.analyze(evaluationOnly = true); advanceUntilIdle()
        assertEquals(0, evaluations.calls)
        vm.selectCv("cv1"); advanceUntilIdle()
        vm.analyze(evaluationOnly = true); advanceUntilIdle()
        assertEquals(listOf("cv1"), cvs.detailCalls)
        assertEquals(2, evaluations.receivedCv!!.skills.size)
        assertEquals("job1", evaluations.receivedOffer!!.id)
        assertEquals("cv1", vm.uiState.value.analysis!!.cvId)
    }

    @Test fun `double taps and repeated open while loading produce only one request`() = runTest(dispatcher) {
        val cvs = EvaluationCvs()
        val evaluations = EvaluationRepository()
        val vm = JobSkillsGapViewModel(cvs, evaluations)
        vm.open(evaluationOffer()); vm.open(evaluationOffer()); vm.loadCvs(); advanceUntilIdle()
        assertEquals(1, cvs.listCalls)
        vm.selectCv("cv1"); vm.selectCv("cv1"); advanceUntilIdle()
        assertEquals(1, cvs.detailCalls.size)
        vm.analyze(evaluationOnly = true); vm.analyze(evaluationOnly = true); advanceUntilIdle()
        assertEquals(1, evaluations.calls)
    }

    @Test fun `changing CV clears results and ignores a late uncooperative evaluation`() = runTest(dispatcher) {
        val evaluations = EvaluationRepository()
        val vm = JobSkillsGapViewModel(EvaluationCvs(), evaluations)
        vm.open(evaluationOffer()); advanceUntilIdle()
        vm.selectCv("cv1"); advanceUntilIdle()
        val delayed = CompletableDeferred<JobSkillsGapAnalysis>()
        evaluations.pending = delayed
        vm.analyze(evaluationOnly = true); runCurrent()
        vm.selectCv("cv2"); advanceUntilIdle()
        assertNull(vm.uiState.value.analysis)
        delayed.complete(evaluationResult()); advanceUntilIdle()
        assertEquals("cv2", vm.uiState.value.selectedCv!!.id)
        assertNull(vm.uiState.value.analysis)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test fun `changing offer ignores stale completion and close cancels all displayed state`() = runTest(dispatcher) {
        val evaluations = EvaluationRepository()
        val vm = JobSkillsGapViewModel(EvaluationCvs(), evaluations)
        vm.open(evaluationOffer()); advanceUntilIdle()
        vm.selectCv("cv1"); advanceUntilIdle()
        val delayed = CompletableDeferred<JobSkillsGapAnalysis>()
        evaluations.pending = delayed
        vm.analyze(evaluationOnly = true); runCurrent()
        vm.open(evaluationOffer().copy(id = "job2")); advanceUntilIdle()
        delayed.complete(evaluationResult()); advanceUntilIdle()
        assertEquals("job2", vm.uiState.value.offer!!.id)
        assertNull(vm.uiState.value.analysis)
        assertNull(vm.uiState.value.selectedCv)
        vm.close()
        assertEquals(JobSkillsGapUiState(), vm.uiState.value)
    }

    @Test fun `failed detail cannot become empty CV and retry loads selected ID`() = runTest(dispatcher) {
        val cvs = EvaluationCvs().apply { detailError = IOException("confidential") }
        val evaluations = EvaluationRepository()
        val vm = JobSkillsGapViewModel(cvs, evaluations)
        vm.open(evaluationOffer()); advanceUntilIdle()
        vm.selectCv("cv1"); advanceUntilIdle()
        vm.analyze(evaluationOnly = true); advanceUntilIdle()
        assertNull(vm.uiState.value.selectedCv)
        assertEquals(0, evaluations.calls)
        assertEquals(R.string.job_gap_network_error, vm.uiState.value.errorMessage)
        cvs.detailError = null
        vm.retry(); advanceUntilIdle()
        assertEquals("cv1", vm.uiState.value.selectedCv!!.id)
    }

    @Test fun `list error retry and no CV state are recoverable without arbitrary selection`() = runTest(dispatcher) {
        val cvs = EvaluationCvs().apply { listError = IOException() }
        val vm = JobSkillsGapViewModel(cvs, EvaluationRepository())
        vm.open(evaluationOffer()); advanceUntilIdle()
        assertNotNull(vm.uiState.value.errorMessage)
        cvs.listError = null
        cvs.items = emptyList()
        vm.retry(); advanceUntilIdle()
        assertTrue(vm.uiState.value.cvs.isEmpty())
        assertEquals(1, vm.uiState.value.cvPage)
        assertNull(vm.uiState.value.selectedCv)
        assertNull(vm.uiState.value.errorMessage)
    }

    @Test fun `all evaluation failures show curated messages and allow retry`() = runTest(dispatcher) {
        val evaluations = EvaluationRepository()
        val vm = JobSkillsGapViewModel(EvaluationCvs(), evaluations)
        vm.open(evaluationOffer()); advanceUntilIdle()
        vm.selectCv("cv1"); advanceUntilIdle()
        for ((failure, message) in listOf(JobSkillsGapFailure.INVALID_CV to R.string.job_gap_invalid_cv,
            JobSkillsGapFailure.INVALID_INPUT to R.string.job_gap_invalid_input,
            JobSkillsGapFailure.UNAUTHORIZED to R.string.skills_gap_unauthorized,
            JobSkillsGapFailure.FORBIDDEN to R.string.job_gap_forbidden,
            JobSkillsGapFailure.ROUTE_UNAVAILABLE to R.string.job_gap_route_unavailable,
            JobSkillsGapFailure.TOO_LARGE to R.string.job_gap_too_large,
            JobSkillsGapFailure.UNAVAILABLE to R.string.job_gap_unavailable,
            JobSkillsGapFailure.INCOMPATIBLE_RESPONSE to R.string.job_gap_incompatible)) {
            evaluations.failure = failure
            vm.analyze(evaluationOnly = true); advanceUntilIdle()
            assertEquals(message, vm.uiState.value.errorMessage)
            assertNull(vm.uiState.value.analysis)
            assertFalse(vm.uiState.value.isLoading)
        }
        evaluations.failure = null
        vm.retry(); advanceUntilIdle()
        assertNotNull(vm.uiState.value.analysis)
    }
}

private class EvaluationRepository : JobSkillsGapRepository {
    var calls = 0
    var receivedCv: Cv? = null
    var receivedOffer: JobOffer? = null
    var failure: JobSkillsGapFailure? = null
    var pending: CompletableDeferred<JobSkillsGapAnalysis>? = null
    override suspend fun analyze(cv: Cv, offer: JobOffer, evaluationOnly: Boolean): JobSkillsGapAnalysis {
        calls++
        receivedCv = cv
        receivedOffer = offer
        failure?.let { throw JobSkillsGapException(it) }
        // Deliberately finish after cancellation to exercise the ViewModel's stale-result guard.
        return pending?.let { withContext(NonCancellable) { it.await() } }
            ?: evaluationResult().copy(cvId = cv.id, jobId = offer.id)
    }
}

private class EvaluationCvs : CvRepository {
    var listCalls = 0
    val detailCalls = mutableListOf<String>()
    var listError: Exception? = null
    var detailError: Exception? = null
    var items = listOf(evaluationCv().copy(skills = emptyList()), evaluationCv().copy(id = "cv2", skills = emptyList()))
    override suspend fun getCvs(page: Int, pageSize: Int, query: String): PagedResult<Cv> {
        listCalls++
        listError?.let { throw it }
        return PagedResult(items, page, pageSize, items.size)
    }
    override suspend fun getCvById(id: String): Cv {
        detailCalls += id
        detailError?.let { throw it }
        return evaluationCv().copy(id = id)
    }
    override suspend fun updateCv(cv: Cv): Cv = error("unused")
    override suspend fun createCv(title: String, templateId: String, personalInfo: UserProfile?): Cv = error("unused")
    override suspend fun deleteCv(id: String) = error("unused")
}
