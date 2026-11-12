package com.cvpro.app.ui.screens.skillsgap

import com.cvpro.app.R
import com.cvpro.app.domain.model.*
import com.cvpro.app.domain.repository.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SkillsGapViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }

    @Test fun disabledStateDoesNotReadCvsOrAnalysis() = runTest(dispatcher) {
        val gaps = FakeGaps(enabled = false)
        val cvs = FakeCvs()
        val vm = SkillsGapViewModel(gaps, cvs)
        advanceUntilIdle()
        vm.onAnalyzeClick(); vm.onRetryClick(); vm.onMoreCvs(); vm.onSelectCv("cv1")
        advanceUntilIdle()
        assertFalse(vm.uiState.value.enabled)
        assertEquals(0, cvs.calls)
        assertTrue(gaps.events.isEmpty())
    }

    @Test fun missingSnapshotAllowsCvSelectionAndExplicitReplaceThenRead() = runTest(dispatcher) {
        val gaps = FakeGaps().apply { readError = SkillsGapException(SkillsGapFailure.INPUT_REQUIRED) }
        val vm = SkillsGapViewModel(gaps, FakeCvs())
        advanceUntilIdle()
        assertEquals(R.string.skills_gap_input_required, vm.uiState.value.errorMessage)
        assertEquals(1, vm.uiState.value.cvs.size)
        vm.onSelectCv("cv1")
        advanceUntilIdle()
        assertEquals("cv1", vm.uiState.value.selectedCv?.id)
        assertNull(vm.uiState.value.input!!.last().level)
        assertNull(gaps.saved)
        gaps.readError = null
        vm.onAnalyzeClick()
        advanceUntilIdle()
        assertEquals(listOf("GET:1", "PUT", "GET:1"), gaps.events)
        assertEquals(listOf(SkillInput("SQL", "beginner"), SkillInput("Python", null)), gaps.saved)
        assertNotNull(vm.uiState.value.analysis)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test fun failedPutDoesNotReadAndRetryDoesNotRepeatWrite() = runTest(dispatcher) {
        val gaps = FakeGaps()
        val vm = SkillsGapViewModel(gaps, FakeCvs())
        advanceUntilIdle()
        vm.onSelectCv("cv1"); advanceUntilIdle()
        gaps.writeError = SkillsGapException(SkillsGapFailure.UNAVAILABLE)
        vm.onAnalyzeClick(); advanceUntilIdle()
        assertEquals(listOf("GET:1", "PUT"), gaps.events)
        assertNull(vm.uiState.value.analysis)
        assertEquals(R.string.skills_gap_unavailable, vm.uiState.value.errorMessage)
        vm.onRetryClick(); advanceUntilIdle()
        assertEquals(listOf("GET:1", "PUT", "GET:1"), gaps.events)
    }

    @Test fun failedReadAfterSuccessfulPutCanRetryWithoutResubmission() = runTest(dispatcher) {
        val gaps = FakeGaps()
        val vm = SkillsGapViewModel(gaps, FakeCvs())
        advanceUntilIdle()
        vm.onSelectCv("cv1"); advanceUntilIdle()
        gaps.readError = SkillsGapException(SkillsGapFailure.UNAVAILABLE)
        vm.onAnalyzeClick(); advanceUntilIdle()
        assertNotNull(gaps.saved)
        assertNull(vm.uiState.value.analysis)
        gaps.readError = null
        vm.onRetryClick(); advanceUntilIdle()
        assertEquals(1, gaps.events.count { it == "PUT" })
        assertNotNull(vm.uiState.value.analysis)
    }

    @Test fun pageNavigationReadsOnlyAndFailureClearsOldResult() = runTest(dispatcher) {
        val gaps = FakeGaps()
        val vm = SkillsGapViewModel(gaps, FakeCvs())
        advanceUntilIdle()
        vm.onPage(2); advanceUntilIdle()
        assertEquals(2, vm.uiState.value.analysis?.page)
        gaps.readError = SkillsGapException(SkillsGapFailure.UNAUTHORIZED)
        vm.onPage(1); advanceUntilIdle()
        assertNull(vm.uiState.value.analysis)
        assertEquals(R.string.skills_gap_unauthorized, vm.uiState.value.errorMessage)
        assertNull(gaps.saved)
    }

    @Test fun unresolvedReasonsHaveUiMessages() {
        val reasons = listOf("MISSING_USER_SKILL", "MISSING_MARKET_REQUIREMENT", "UNKNOWN_LEVEL",
            "UNKNOWN_SCALE", "SCALE_MISMATCH")
        assertEquals(5, reasons.map(::unresolvedReasonMessage).distinct().size)
        assertEquals(R.string.skills_gap_unresolved_other, unresolvedReasonMessage("FUTURE_REASON"))
    }

    @Test fun cvServiceFailureDoesNotPreventReadingExistingSnapshot() = runTest(dispatcher) {
        val gaps = FakeGaps()
        val vm = SkillsGapViewModel(gaps, FakeCvs().apply { listError = java.io.IOException() })
        advanceUntilIdle()
        assertEquals(R.string.skills_gap_error_network, vm.uiState.value.cvErrorMessage)
        assertNull(vm.uiState.value.errorMessage)
        assertNotNull(vm.uiState.value.analysis)
        assertEquals(listOf("GET:1"), gaps.events)
    }
}

private class FakeGaps(override val enabled: Boolean = true) : SkillsGapRepository {
    val events = mutableListOf<String>()
    var readError: Exception? = null
    var writeError: Exception? = null
    var saved: List<SkillInput>? = null
    override suspend fun getAnalysis(page: Int): SkillsGapAnalysis {
        events += "GET:$page"
        readError?.let { throw it }
        return SkillsGapAnalysis(emptyList(), emptyList(), status = "incomplete", page = page)
    }
    override suspend fun replaceInput(skills: List<SkillInput>) {
        events += "PUT"
        writeError?.let { throw it }
        saved = skills
    }
}

private class FakeCvs : CvRepository {
    var calls = 0
    var listError: Exception? = null
    private val cv = Cv("cv1", "user", "My CV", "template", "", "", null,
        UserProfile("user", "Name", "email", null, "", null, null, emptyList(), emptyList(), emptyList()),
        emptyList(), emptyList(), listOf(Skill("1", "SQL", "beginner", ""), Skill("2", "Python", "NEW", "")), emptyList(), emptyList())
    override suspend fun getCvs(page: Int, pageSize: Int, query: String): PagedResult<Cv> {
        calls++
        listError?.let { throw it }
        return PagedResult(listOf(cv), page, pageSize, 1)
    }
    override suspend fun getCvById(id: String): Cv { calls++; return cv }
    override suspend fun updateCv(cv: Cv): Cv = error("not used")
    override suspend fun createCv(title: String, templateId: String, personalInfo: UserProfile?): Cv = error("not used")
    override suspend fun deleteCv(id: String) = error("not used")
}
