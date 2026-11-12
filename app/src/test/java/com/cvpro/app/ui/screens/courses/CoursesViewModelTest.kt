package com.cvpro.app.ui.screens.courses

import com.cvpro.app.domain.model.*
import com.cvpro.app.domain.repository.CourseRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class CoursesViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }
    @Test fun initialSkillIsExactAndEmptyFilteredCatalogHasItsOwnState() = runTest(dispatcher) {
        val repo = CatalogRepository()
        val vm = CoursesViewModel(repo)
        vm.start("C++"); vm.start("ignored")
        assertTrue(vm.uiState.value.loading)
        advanceUntilIdle()
        assertEquals(CourseFilters(skillName = "C++"), vm.uiState.value.filters)
        assertTrue(vm.uiState.value.filters.active)
        assertEquals(0L, vm.uiState.value.result!!.totalRecords)
        assertTrue(vm.uiState.value.result!!.items.isEmpty())
        vm.applyFilters(CourseFilters()); advanceUntilIdle()
        assertFalse(vm.uiState.value.filters.active)
        assertEquals(2, repo.calls.size)
    }
    @Test fun errorsRetainPriorPageAndRetryTheRequestedPage() = runTest(dispatcher) {
        for (failure in CourseFailure.entries) {
            val repo = CatalogRepository().apply { count = 21 }
            val vm = CoursesViewModel(repo)
            vm.start(null); advanceUntilIdle()
            assertTrue(vm.uiState.value.result!!.hasMore)
            repo.failure = CourseException(failure)
            vm.load(2); advanceUntilIdle()
            assertEquals(failure, vm.uiState.value.error)
            assertEquals(1, vm.uiState.value.result!!.page)
            assertFalse(vm.uiState.value.loading)
            repo.failure = null
            vm.retry(); advanceUntilIdle()
            assertNull(vm.uiState.value.error)
            assertEquals(2, vm.uiState.value.result!!.page)
            assertEquals(21L, vm.uiState.value.result!!.totalRecords)
            assertFalse(vm.uiState.value.result!!.hasMore)
        }
    }
    @Test fun changingFiltersCancelsOldPageEvenWhenTransportIgnoresCancellation() = runTest(dispatcher) {
        val repo = CatalogRepository()
        val pending = CompletableDeferred<CoursePage>()
        repo.pending = pending
        val vm = CoursesViewModel(repo)
        vm.start(null); runCurrent()
        repo.pending = null
        vm.applyFilters(CourseFilters(platform = "  Exact Provider  ", isPaid = false)); runCurrent()
        pending.complete(CoursePage(emptyList(), 9, 20, 999)); advanceUntilIdle()
        assertEquals(1, vm.uiState.value.result!!.page)
        assertEquals(0L, vm.uiState.value.result!!.totalRecords)
        assertEquals("  Exact Provider  ", repo.calls.last().second.platform)
        assertEquals(false, repo.calls.last().second.isPaid)
    }
}
private class CatalogRepository : CourseRepository {
    val calls = mutableListOf<Pair<Int, CourseFilters>>()
    var count = 0L
    var failure: Exception? = null
    var pending: CompletableDeferred<CoursePage>? = null
    override suspend fun getCourses(page: Int, pageSize: Int, filters: CourseFilters): CoursePage {
        calls += page to filters
        failure?.let { throw it }
        pending?.let { return withContext(NonCancellable) { it.await() } }
        return CoursePage(emptyList(), page, pageSize, count)
    }
}
