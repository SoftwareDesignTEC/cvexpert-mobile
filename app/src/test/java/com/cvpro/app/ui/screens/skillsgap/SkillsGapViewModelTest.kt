package com.cvpro.app.ui.screens.skillsgap

import com.cvpro.app.R
import com.cvpro.app.domain.model.SkillInput
import com.cvpro.app.domain.model.SkillLevel
import com.cvpro.app.domain.model.SkillsGapAnalysis
import com.cvpro.app.domain.model.SkillsGapException
import com.cvpro.app.domain.model.SkillsGapFailure
import com.cvpro.app.domain.repository.SkillsGapRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SkillsGapViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `missing snapshot guides manual input and only confirmation writes then reloads`() = runTest(dispatcher) {
        val repository = FakeSkillsGapRepository().apply { readFailure = SkillsGapFailure.INPUT_REQUIRED }
        val viewModel = SkillsGapViewModel(repository)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.inputRequired)
        assertEquals(R.string.skills_gap_input_required, viewModel.uiState.value.errorMessage)

        viewModel.openInput()
        viewModel.changeName(0, " Example A ")
        viewModel.changeLevel(0, SkillLevel.ADVANCED)
        viewModel.addSkill()
        viewModel.changeName(1, "Example B")
        viewModel.confirmInput()
        viewModel.reviewInput()
        advanceUntilIdle()

        assertEquals(SkillsInputStage.REVIEW, viewModel.uiState.value.inputStage)
        assertEquals(listOf("GET:1"), repository.events)

        repository.readFailure = null
        viewModel.confirmInput()
        viewModel.confirmInput()
        advanceUntilIdle()

        assertEquals(listOf("GET:1", "PUT", "GET:1"), repository.events)
        assertEquals(listOf(SkillInput("Example A", SkillLevel.ADVANCED), SkillInput("Example B", null)), repository.saved)
        assertFalse(viewModel.uiState.value.inputRequired)
        assertNotNull(viewModel.uiState.value.analysis)
    }

    @Test
    fun `empty and duplicate drafts cannot enter review or write`() = runTest(dispatcher) {
        val repository = FakeSkillsGapRepository()
        val viewModel = SkillsGapViewModel(repository)
        advanceUntilIdle()
        viewModel.openInput()
        viewModel.removeSkill(0)
        viewModel.reviewInput()
        viewModel.confirmInput()
        assertEquals(R.string.skills_gap_invalid_input, viewModel.uiState.value.inputError)

        viewModel.addSkill()
        viewModel.addSkill()
        viewModel.changeName(0, "Example")
        viewModel.changeName(1, " Example ")
        viewModel.reviewInput()
        viewModel.confirmInput()
        advanceUntilIdle()
        assertEquals(SkillsInputStage.EDIT, viewModel.uiState.value.inputStage)
        assertEquals(R.string.skills_gap_invalid_input, viewModel.uiState.value.inputError)
        assertEquals(listOf("GET:1"), repository.events)
    }

    @Test
    fun `closing review preserves draft without writing over existing skills`() = runTest(dispatcher) {
        val repository = FakeSkillsGapRepository()
        val viewModel = SkillsGapViewModel(repository)
        advanceUntilIdle()
        viewModel.prepareInput()
        viewModel.closeInput()
        viewModel.confirmInput()
        advanceUntilIdle()

        assertEquals(listOf("GET:1"), repository.events)
        assertNull(viewModel.uiState.value.inputStage)
        assertEquals(listOf(SkillInput("Example", null)), viewModel.uiState.value.draft)
        assertNotNull(viewModel.uiState.value.analysis)
    }

    @Test
    fun `failed write preserves draft and retry reads only`() = runTest(dispatcher) {
        val repository = FakeSkillsGapRepository().apply { writeFailure = SkillsGapFailure.UNAVAILABLE }
        val viewModel = SkillsGapViewModel(repository)
        advanceUntilIdle()
        viewModel.prepareInput()
        viewModel.confirmInput()
        advanceUntilIdle()

        assertEquals(listOf("GET:1", "PUT"), repository.events)
        assertEquals(SkillsInputStage.EDIT, viewModel.uiState.value.inputStage)
        assertEquals(R.string.skills_gap_unavailable, viewModel.uiState.value.inputError)
        assertEquals(listOf(SkillInput("Example", null)), viewModel.uiState.value.draft)
        assertNull(viewModel.uiState.value.analysis)

        viewModel.closeInput()
        assertEquals(R.string.skills_gap_unavailable, viewModel.uiState.value.errorMessage)
        viewModel.onRetryClick()
        advanceUntilIdle()
        assertEquals(listOf("GET:1", "PUT", "GET:1"), repository.events)
    }

    @Test
    fun `after confirmed write a failed reload can be retried without writing again`() = runTest(dispatcher) {
        val repository = FakeSkillsGapRepository()
        val viewModel = SkillsGapViewModel(repository)
        advanceUntilIdle()
        viewModel.prepareInput()
        repository.readFailure = SkillsGapFailure.UNAVAILABLE
        viewModel.confirmInput()
        advanceUntilIdle()
        assertNotNull(repository.saved)
        assertEquals(R.string.skills_gap_unavailable, viewModel.uiState.value.errorMessage)
        assertNull(viewModel.uiState.value.analysis)

        repository.readFailure = null
        viewModel.onRetryClick()
        advanceUntilIdle()
        assertEquals(listOf("GET:1", "PUT", "GET:1", "GET:1"), repository.events)
        assertNotNull(viewModel.uiState.value.analysis)
    }

    private fun SkillsGapViewModel.prepareInput() {
        openInput()
        changeName(0, "Example")
        reviewInput()
    }
}

private class FakeSkillsGapRepository : SkillsGapRepository {
    val events = mutableListOf<String>()
    var readFailure: SkillsGapFailure? = null
    var writeFailure: SkillsGapFailure? = null
    var saved: List<SkillInput>? = null

    override suspend fun getAnalysis(page: Long): SkillsGapAnalysis {
        events += "GET:$page"
        readFailure?.let { throw SkillsGapException(it) }
        return SkillsGapAnalysis(
            skills = emptyList(),
            recommendations = emptyList(),
            status = "incomplete",
            comparedSkills = 0,
            scaleKey = "cv-skill-v1",
            marketStatus = "missing",
            unresolved = emptyList(),
            page = page,
            pageSize = 100,
            totalRecords = 0,
        )
    }

    override suspend fun replaceInput(skills: List<SkillInput>) {
        events += "PUT"
        writeFailure?.let { throw SkillsGapException(it) }
        saved = skills
    }
}
