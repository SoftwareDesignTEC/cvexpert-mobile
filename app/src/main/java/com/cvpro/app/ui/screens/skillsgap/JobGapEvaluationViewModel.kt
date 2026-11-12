package com.cvpro.app.ui.screens.skillsgap

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cvpro.app.R
import com.cvpro.app.domain.model.*
import com.cvpro.app.domain.repository.CvRepository
import com.cvpro.app.domain.repository.JobGapEvaluationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

data class JobGapEvaluationUiState(
    val offer: JobOffer? = null, val cvs: List<Cv> = emptyList(),
    val cvPage: Int = 0, val hasMoreCvs: Boolean = false,
    val selectedCvId: String? = null, val selectedCv: Cv? = null,
    val isLoading: Boolean = false, val evaluation: JobGapEvaluation? = null,
    @param:StringRes val errorMessage: Int? = null,
)

@HiltViewModel
class JobGapEvaluationViewModel @Inject constructor(
    private val cvs: CvRepository,
    private val evaluations: JobGapEvaluationRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(JobGapEvaluationUiState())
    val uiState = _uiState.asStateFlow()
    private var operation: Job? = null
    private var retryAction: (() -> Unit)? = null

    fun open(offer: JobOffer) {
        if (_uiState.value.offer == offer) return
        close()
        _uiState.value = JobGapEvaluationUiState(offer = offer)
        loadCvs()
    }

    fun close() {
        operation?.cancel()
        retryAction = null
        _uiState.value = JobGapEvaluationUiState()
    }

    fun loadCvs() {
        val state = _uiState.value
        if (state.isLoading || state.offer == null || (state.cvPage > 0 && !state.hasMoreCvs)) return
        runOperation(::loadCvs) {
            val page = cvs.getCvs(state.cvPage + 1, 20, "")
            coroutineContext.ensureActive()
            _uiState.update { it.copy(cvs = (it.cvs + page.items).distinctBy { cv -> cv.id },
                cvPage = page.page, hasMoreCvs = page.hasMore) }
        }
    }

    fun selectCv(id: String) {
        val state = _uiState.value
        if (state.cvs.none { it.id == id } || (state.isLoading && state.selectedCvId == id)) return
        operation?.cancel()
        _uiState.update { it.copy(selectedCvId = id, selectedCv = null, evaluation = null) }
        runOperation({ selectCv(id) }) {
            val cv = cvs.getCvById(id)
            coroutineContext.ensureActive()
            if (cv.id != id) throw JobGapException(JobGapFailure.INCOMPATIBLE)
            _uiState.update { it.copy(selectedCv = cv) }
        }
    }

    fun evaluate() {
        val state = _uiState.value
        val cv = state.selectedCv ?: return
        val offer = state.offer ?: return
        if (state.isLoading) return
        _uiState.update { it.copy(evaluation = null) }
        runOperation(::evaluate) {
            val result = evaluations.evaluate(cv, offer)
            coroutineContext.ensureActive()
            _uiState.update { it.copy(evaluation = result) }
        }
    }

    fun retry() {
        if (!_uiState.value.isLoading) retryAction?.invoke()
    }

    private fun runOperation(retry: () -> Unit, work: suspend () -> Unit) {
        retryAction = retry
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        operation = viewModelScope.launch {
            try {
                work()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                coroutineContext.ensureActive()
                _uiState.update { it.copy(errorMessage = error.jobGapMessage()) }
            } finally {
                if (coroutineContext[Job]?.isActive == true) _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}

@StringRes
private fun Exception.jobGapMessage(): Int {
    val failure = when (this) {
        is JobGapException -> failure
        is HttpException -> when (code()) {
            400 -> JobGapFailure.INVALID_INPUT
            401 -> JobGapFailure.UNAUTHORIZED
            403 -> JobGapFailure.FORBIDDEN
            404 -> JobGapFailure.NOT_FOUND
            413 -> JobGapFailure.TOO_LARGE
            else -> JobGapFailure.UNAVAILABLE
        }
        else -> null
    }
    return when (failure) {
        JobGapFailure.INVALID_CV -> R.string.job_gap_invalid_cv
        JobGapFailure.INVALID_INPUT -> R.string.job_gap_invalid_input
        JobGapFailure.UNAUTHORIZED -> R.string.skills_gap_unauthorized
        JobGapFailure.FORBIDDEN -> R.string.job_gap_forbidden
        JobGapFailure.NOT_FOUND -> R.string.job_gap_not_found
        JobGapFailure.TOO_LARGE -> R.string.job_gap_too_large
        JobGapFailure.UNAVAILABLE -> R.string.job_gap_unavailable
        JobGapFailure.INCOMPATIBLE -> R.string.job_gap_incompatible
        null -> if (this is IOException) R.string.job_gap_network else R.string.job_gap_unavailable
    }
}
