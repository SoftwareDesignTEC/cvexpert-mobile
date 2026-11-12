package com.cvpro.app.ui.screens.skillsgap

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cvpro.app.R
import com.cvpro.app.domain.model.*
import com.cvpro.app.domain.repository.CvRepository
import com.cvpro.app.domain.repository.JobOfferRepository
import com.cvpro.app.domain.repository.JobSkillsGapRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

data class JobSkillsGapUiState(
    val offer: JobOffer? = null,
    val offerLocked: Boolean = false,
    val cvs: List<Cv> = emptyList(),
    val cvPage: Int = 0,
    val hasMoreCvs: Boolean = false,
    val selectedCvId: String? = null,
    val selectedCv: Cv? = null,
    val offers: List<JobOffer> = emptyList(),
    val offerQuery: String = "",
    val offerPage: Int = 0,
    val hasMoreOffers: Boolean = false,
    val isLoadingOffers: Boolean = false,
    @param:StringRes val offersError: Int? = null,
    val isLoading: Boolean = false,
    val analysis: JobSkillsGapAnalysis? = null,
    @param:StringRes val errorMessage: Int? = null,
    val canEvaluateOnly: Boolean = false,
    val needsCvCorrection: Boolean = false,
)

/** Shared by the main offer mode and the sheet opened from Jobs. Never writes market snapshots. */
@HiltViewModel
class JobSkillsGapViewModel @Inject constructor(
    private val cvs: CvRepository,
    private val gaps: JobSkillsGapRepository,
    private val jobs: JobOfferRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(JobSkillsGapUiState())
    val uiState = _uiState.asStateFlow()
    private var initialized = false
    private var operation: Job? = null
    private var offersOperation: Job? = null
    private var retryAction: (() -> Unit)? = null

    fun start() {
        if (initialized) return
        initialized = true
        loadCvs()
    }

    fun open(offer: JobOffer) {
        if (initialized && _uiState.value.offerLocked && _uiState.value.offer == offer) return
        close()
        initialized = true
        _uiState.value = JobSkillsGapUiState(offer = offer, offerLocked = true)
        loadCvs()
    }

    fun close() {
        operation?.cancel()
        offersOperation?.cancel()
        retryAction = null
        initialized = false
        _uiState.value = JobSkillsGapUiState()
    }

    fun loadCvs() {
        val state = _uiState.value
        if (!initialized || state.isLoading || (state.cvPage > 0 && !state.hasMoreCvs)) return
        runOperation(R.string.job_gap_cv_list_error, ::loadCvs) {
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
        _uiState.update { it.copy(selectedCvId = id, selectedCv = null, analysis = null) }
        runOperation(R.string.job_gap_cv_detail_error, { selectCv(id) }) {
            val cv = cvs.getCvById(id)
            coroutineContext.ensureActive()
            if (cv.id != id) throw JobSkillsGapException(JobSkillsGapFailure.INCOMPATIBLE_RESPONSE)
            _uiState.update { it.copy(selectedCv = cv) }
            if (!_uiState.value.offerLocked && _uiState.value.offerPage == 0) loadOffers()
        }
    }

    fun selectOffer(id: String) {
        val state = _uiState.value
        if (state.offerLocked || state.selectedCv == null) return
        val offer = state.offers.find { it.id == id } ?: return
        operation?.cancel()
        retryAction = null
        _uiState.update { it.copy(offer = offer, analysis = null, isLoading = false,
            errorMessage = null, canEvaluateOnly = false, needsCvCorrection = false) }
    }

    fun changeOfferQuery(query: String) {
        if (_uiState.value.offerLocked || _uiState.value.selectedCv == null || query == _uiState.value.offerQuery) return
        offersOperation?.cancel()
        _uiState.update { it.copy(offerQuery = query, offers = emptyList(), offerPage = 0,
            hasMoreOffers = false, isLoadingOffers = true, offersError = null) }
        offersOperation = viewModelScope.launch {
            delay(350)
            fetchOffers(page = 1, query = query)
        }
    }

    fun loadOffers() {
        val state = _uiState.value
        if (state.offerLocked || state.selectedCv == null || state.isLoadingOffers ||
            (state.offerPage > 0 && !state.hasMoreOffers)) return
        _uiState.update { it.copy(isLoadingOffers = true, offersError = null) }
        offersOperation = viewModelScope.launch { fetchOffers(state.offerPage + 1, state.offerQuery) }
    }

    private suspend fun fetchOffers(page: Int, query: String) {
        try {
            val result = jobs.getOffers(page = page, pageSize = 10, query = query)
            coroutineContext.ensureActive()
            _uiState.update { it.copy(offers = (if (page == 1) result.items else it.offers + result.items)
                .distinctBy { offer -> offer.id }, offerPage = result.page, hasMoreOffers = result.hasMore) }
        } catch (error: CancellationException) { throw error }
        catch (error: Exception) {
            coroutineContext.ensureActive()
            _uiState.update { it.copy(offersError = error.jobGapMessage(R.string.gap_offers_error)) }
        } finally {
            if (coroutineContext[Job]?.isActive == true) _uiState.update { it.copy(isLoadingOffers = false) }
        }
    }

    fun analyze(evaluationOnly: Boolean = false) {
        val state = _uiState.value
        val cv = state.selectedCv ?: return
        val offer = state.offer ?: return
        if (state.isLoading) return
        _uiState.update { it.copy(analysis = null) }
        runOperation(R.string.job_gap_error_generic, { analyze(evaluationOnly) }) {
            val result = gaps.analyze(cv, offer, evaluationOnly)
            coroutineContext.ensureActive()
            _uiState.update { it.copy(analysis = result) }
            if (!evaluationOnly) {
                // After courses succeed, an explicit retry repeats only the failed evaluation POST.
                retryAction = { retryLevels(cv, offer, result) }
                addLevels(cv, offer, result)
            }
        }
    }

    private fun retryLevels(cv: Cv, offer: JobOffer, courses: JobSkillsGapAnalysis) {
        runOperation(R.string.job_gap_error_generic, { retryLevels(cv, offer, courses) }) {
            addLevels(cv, offer, courses)
        }
    }

    private suspend fun addLevels(cv: Cv, offer: JobOffer, courses: JobSkillsGapAnalysis) {
        val levels = gaps.analyze(cv, offer, evaluationOnly = true)
        coroutineContext.ensureActive()
        if (levels.cvId != courses.cvId || levels.jobId != courses.jobId ||
            levels.matchedSkills != courses.matchedSkills || levels.notDocumentedSkills != courses.notDocumentedSkills)
            throw JobSkillsGapException(JobSkillsGapFailure.INCOMPATIBLE_RESPONSE)
        _uiState.update { it.copy(analysis = courses.copy(assessments = levels.assessments, levelAnalysis = levels.levelAnalysis)) }
    }

    fun retry() { if (!_uiState.value.isLoading) retryAction?.invoke() }

    private fun runOperation(@StringRes fallback: Int, retry: () -> Unit, work: suspend () -> Unit) {
        retryAction = retry
        _uiState.update { it.copy(isLoading = true, errorMessage = null, canEvaluateOnly = false, needsCvCorrection = false) }
        operation = viewModelScope.launch {
            try { work() }
            catch (error: CancellationException) { throw error }
            catch (error: Exception) {
                coroutineContext.ensureActive()
                val failure = (error as? JobSkillsGapException)?.failure
                _uiState.update { it.copy(errorMessage = error.jobGapMessage(fallback),
                    canEvaluateOnly = failure == JobSkillsGapFailure.CATALOG_UNAVAILABLE,
                    needsCvCorrection = failure in setOf(JobSkillsGapFailure.INVALID_CV_LEVEL, JobSkillsGapFailure.INVALID_CV)) }
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
        JobSkillsGapFailure.INVALID_CV -> R.string.job_gap_invalid_cv
        JobSkillsGapFailure.FORBIDDEN -> R.string.job_gap_forbidden
        JobSkillsGapFailure.TOO_LARGE -> R.string.job_gap_too_large
        JobSkillsGapFailure.INVALID_INPUT -> R.string.job_gap_invalid_input
        JobSkillsGapFailure.UNAUTHORIZED -> R.string.skills_gap_unauthorized
        JobSkillsGapFailure.ROUTE_UNAVAILABLE -> R.string.job_gap_route_unavailable
        JobSkillsGapFailure.UNAVAILABLE -> R.string.job_gap_unavailable
        JobSkillsGapFailure.CATALOG_UNAVAILABLE -> R.string.job_gap_catalog_error
        JobSkillsGapFailure.INCOMPATIBLE_RESPONSE -> R.string.job_gap_incompatible
    }
    this is HttpException && code() == 401 -> R.string.skills_gap_unauthorized
    this is HttpException && code() == 403 -> R.string.job_gap_forbidden
    this is HttpException && code() == 404 -> R.string.job_gap_route_unavailable
    this is IOException -> R.string.job_gap_network_error
    else -> fallback
}
