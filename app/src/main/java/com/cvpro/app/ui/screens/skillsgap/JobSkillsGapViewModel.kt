package com.cvpro.app.ui.screens.skillsgap

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cvpro.app.R
import com.cvpro.app.domain.model.*
import com.cvpro.app.domain.repository.CvRepository
import com.cvpro.app.domain.repository.JobSkillsGapRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

data class JobSkillsGapUiState(
    val offer: JobOffer? = null,
    val cvs: List<Cv> = emptyList(),
    val cvPage: Int = 0,
    val hasMoreCvs: Boolean = false,
    val selectedCvId: String? = null,
    val selectedCv: Cv? = null,
    val isLoading: Boolean = false,
    val analysis: JobSkillsGapAnalysis? = null,
    @param:StringRes val errorMessage: Int? = null,
    val canEvaluateOnly: Boolean = false,
    val needsCvCorrection: Boolean = false,
)

@HiltViewModel
class JobSkillsGapViewModel @Inject constructor(
    private val cvs: CvRepository,
    private val gaps: JobSkillsGapRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(JobSkillsGapUiState())
    val uiState = _uiState.asStateFlow()
    private var operation: Job? = null

    fun open(offer: JobOffer) {
        operation?.cancel()
        _uiState.value = JobSkillsGapUiState(offer = offer)
        loadCvs()
    }

    fun close() {
        operation?.cancel()
        _uiState.value = JobSkillsGapUiState()
    }

    fun loadCvs() {
        if (_uiState.value.isLoading) return
        val page = _uiState.value.cvPage + 1
        runOperation(R.string.job_gap_cv_list_error) {
            val result = cvs.getCvs(page, 20, "")
            coroutineContext.ensureActive()
            _uiState.update { it.copy(
                cvs = (it.cvs + result.items).distinctBy { cv -> cv.id },
                cvPage = result.page, hasMoreCvs = result.hasMore,
            ) }
        }
    }

    fun selectCv(id: String) {
        if (_uiState.value.cvs.none { it.id == id }) return
        operation?.cancel()
        _uiState.update { it.copy(
            selectedCvId = id, selectedCv = null, analysis = null, canEvaluateOnly = false,
            needsCvCorrection = false,
        ) }
        runOperation(R.string.job_gap_cv_detail_error) {
            val cv = cvs.getCvById(id)
            coroutineContext.ensureActive()
            if (cv.id != id) throw JobSkillsGapException(JobSkillsGapFailure.INCOMPATIBLE_RESPONSE)
            _uiState.update { it.copy(selectedCv = cv) }
        }
    }

    fun analyze(evaluationOnly: Boolean = false) {
        val state = _uiState.value
        val cv = state.selectedCv ?: return
        val offer = state.offer ?: return
        if (state.isLoading || (evaluationOnly && !state.canEvaluateOnly)) return
        _uiState.update { it.copy(analysis = null, canEvaluateOnly = false, needsCvCorrection = false) }
        runOperation(R.string.job_gap_error_generic) {
            val result = gaps.analyze(cv, offer, evaluationOnly)
            coroutineContext.ensureActive()
            _uiState.update { it.copy(analysis = result) }
        }
    }

    private fun runOperation(@StringRes fallback: Int, work: suspend () -> Unit) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        operation = viewModelScope.launch {
            try {
                work()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                coroutineContext.ensureActive()
                val failure = (error as? JobSkillsGapException)?.failure
                _uiState.update { it.copy(
                    errorMessage = error.jobGapMessage(fallback),
                    canEvaluateOnly = failure == JobSkillsGapFailure.CATALOG_UNAVAILABLE,
                    needsCvCorrection = failure == JobSkillsGapFailure.INVALID_CV_LEVEL,
                ) }
            } finally {
                if (coroutineContext[Job]?.isActive == true) _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}

@StringRes
private fun Exception.jobGapMessage(@StringRes fallback: Int): Int = when {
    this is JobSkillsGapException -> when (failure) {
        JobSkillsGapFailure.INVALID_CV_LEVEL -> R.string.job_gap_invalid_level
        JobSkillsGapFailure.INVALID_INPUT -> R.string.job_gap_invalid_input
        JobSkillsGapFailure.UNAUTHORIZED -> R.string.skills_gap_unauthorized
        JobSkillsGapFailure.ROUTE_UNAVAILABLE -> R.string.job_gap_route_unavailable
        JobSkillsGapFailure.UNAVAILABLE -> R.string.job_gap_unavailable
        JobSkillsGapFailure.CATALOG_UNAVAILABLE -> R.string.job_gap_catalog_error
        JobSkillsGapFailure.INCOMPATIBLE_RESPONSE -> R.string.job_gap_incompatible
    }
    this is HttpException && code() == 401 -> R.string.skills_gap_unauthorized
    this is IOException -> R.string.job_gap_network_error
    else -> fallback
}
