package com.cvpro.app.ui.screens.courses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cvpro.app.domain.model.*
import com.cvpro.app.domain.repository.CourseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class CoursesUiState(
    val filters: CourseFilters = CourseFilters(), val result: CoursePage? = null,
    val loading: Boolean = false, val error: CourseFailure? = null,
)
@HiltViewModel
class CoursesViewModel @Inject constructor(private val repository: CourseRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(CoursesUiState())
    val uiState = _uiState.asStateFlow()
    private var operation: Job? = null
    private var initialized = false
    private var requestedPage = 1
    fun start(skillName: String?) {
        if (initialized) return
        initialized = true
        applyFilters(CourseFilters(skillName = skillName))
    }
    fun applyFilters(filters: CourseFilters) {
        operation?.cancel()
        _uiState.value = CoursesUiState(filters = filters)
        load(1)
    }
    fun load(page: Int) {
        if (page < 1) return
        operation?.cancel()
        requestedPage = page
        val filters = _uiState.value.filters
        _uiState.update { it.copy(loading = true, error = null) }
        operation = viewModelScope.launch {
            try {
                val result = repository.getCourses(page, 20, filters)
                currentCoroutineContext().ensureActive()
                _uiState.update { it.copy(result = result, loading = false) }
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) {
                currentCoroutineContext().ensureActive()
                _uiState.update { it.copy(loading = false, error = (error as? CourseException)?.failure ?: CourseFailure.UNAVAILABLE) }
            }
        }
    }
    fun retry() = load(requestedPage)
}
