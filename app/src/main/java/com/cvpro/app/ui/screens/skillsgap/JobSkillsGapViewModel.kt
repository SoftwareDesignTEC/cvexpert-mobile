package com.cvpro.app.ui.screens.skillsgap

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cvpro.app.R
import com.cvpro.app.domain.model.*
import com.cvpro.app.domain.repository.CourseRepository
import com.cvpro.app.domain.repository.CvRepository
import com.cvpro.app.domain.repository.JobOfferRepository
import com.cvpro.app.domain.repository.JobSkillsGapRepository
import com.cvpro.app.domain.repository.UnsafeCvUpdateException
import com.cvpro.app.ui.components.FormValidation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
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
    val levelCourses: Map<String, LevelCoursesState> = emptyMap(),
    @param:StringRes val errorMessage: Int? = null,
    val canEvaluateOnly: Boolean = false,
    val needsCvCorrection: Boolean = false,
    val skillToAdd: String? = null,
    @param:StringRes val skillSaveError: Int? = null,
    @param:StringRes val skillNotice: Int? = null,
)

data class LevelCoursesState(
    val items: List<Course> = emptyList(), val page: Int = 0, val totalRecords: Long = 0,
    val loading: Boolean = false, val error: CourseFailure? = null, val excludedRecords: Int = 0,
) {
    val hasMore get() = page.toLong() * 5 < totalRecords
}

/** Shared by the main offer mode and the sheet opened from Jobs. Never writes market snapshots. */
@HiltViewModel
class JobSkillsGapViewModel @Inject constructor(
    private val cvs: CvRepository,
    private val gaps: JobSkillsGapRepository,
    private val jobs: JobOfferRepository,
    private val courses: CourseRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(JobSkillsGapUiState())
    val uiState = _uiState.asStateFlow()
    private val courseOperations = mutableMapOf<String, Job>()
    private val courseSlots = Semaphore(2)
    // Each analysis owns its course requests. A late transport response cannot populate another selection.
    private var courseGeneration = 0
    private fun resetCourses() {
        courseGeneration++
        courseOperations.values.forEach { it.cancel() }
        courseOperations.clear()
        _uiState.update { it.copy(levelCourses = emptyMap()) }
    }

    fun loadLevelCourses(skillName: String, nextPage: Boolean = false) {
        if (_uiState.value.analysis?.assessments.orEmpty().none { it.name == skillName && it.canSuggestLevelCourses() }) return
        val previous = _uiState.value.levelCourses[skillName] ?: LevelCoursesState()
        if (previous.loading || (previous.page > 0 && previous.error == null && !nextPage) || (nextPage && !previous.hasMore)) return
        val page = if (nextPage || previous.error != null) previous.page + 1 else 1
        val generation = courseGeneration
        _uiState.update { it.copy(levelCourses = it.levelCourses + (skillName to previous.copy(loading = true, error = null))) }
        courseOperations[skillName] = viewModelScope.launch {
            try {
                val result = courseSlots.withPermit {
                    courses.getCourses(page, 5, CourseFilters(skillName = skillName))
                }
                currentCoroutineContext().ensureActive()
                if (generation != courseGeneration) return@launch
                _uiState.update { it.copy(levelCourses = it.levelCourses + (skillName to LevelCoursesState(
                    (previous.items + result.items).distinctBy { course -> course.id }, result.page, result.totalRecords,
                    excludedRecords = previous.excludedRecords + result.excludedRecords))) }
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) {
                currentCoroutineContext().ensureActive()
                if (generation == courseGeneration) _uiState.update { it.copy(levelCourses = it.levelCourses +
                    (skillName to previous.copy(error = (error as? CourseException)?.failure ?: CourseFailure.UNAVAILABLE))) }
            }
        }
    }

    private var initialized = false
    private var operation: Job? = null
    private var offersOperation: Job? = null
    private var retryAction: (() -> Unit)? = null
    private var skillUpdateInProgress = false
    private var lastEvaluationOnly = false

    fun chooseMissingSkill(name: String) {
        val state = _uiState.value
        if (state.isLoading || name !in state.analysis?.notDocumentedSkills.orEmpty()) return
        _uiState.update { it.copy(skillToAdd = name, skillSaveError = null, skillNotice = null) }
    }

    fun cancelAddSkill() {
        if (!_uiState.value.isLoading) _uiState.update { it.copy(skillToAdd = null, skillSaveError = null) }
    }

    fun addMissingSkill(level: String) {
        val state = _uiState.value
        val name = state.skillToAdd ?: return
        val id = state.selectedCvId ?: return
        val offer = state.offer ?: return
        if (state.isLoading || FormValidation.required(name) != null || FormValidation.skillLevel(level) != null) return
        skillUpdateInProgress = true
        _uiState.update { it.copy(skillSaveError = null) }
        runOperation(R.string.job_gap_error_generic, { addMissingSkill(level) }) {
            try {
                val latest = try {
                    cvs.getCvForSkillUpdate(id).also {
                        if (it.id != id) throw UnsafeCvUpdateException()
                    }
                } catch (error: CancellationException) { throw error }
                catch (error: Exception) {
                    coroutineContext.ensureActive()
                    _uiState.update { it.copy(skillSaveError = if (error is UnsafeCvUpdateException)
                        R.string.gap_skill_unsafe else error.jobGapMessage(R.string.gap_skill_save_error)) }
                    return@runOperation
                }
                coroutineContext.ensureActive()
                val duplicate = latest.skills.any { it.name.trim().equals(name.trim(), ignoreCase = true) }
                val saved = try {
                    if (duplicate) latest else cvs.updateCv(latest.copy(skills = latest.skills +
                        Skill(id = "", name = name.trim(), level = level, category = "")))
                } catch (error: CancellationException) { throw error }
                catch (error: Exception) {
                    coroutineContext.ensureActive()
                    _uiState.update { it.copy(skillSaveError = error.jobGapMessage(R.string.gap_skill_save_error)) }
                    return@runOperation
                }
                coroutineContext.ensureActive()
                // From here on, retry only reads and analyzes. Never repeat a successful PUT.
                resetCourses()
                _uiState.update { it.copy(skillToAdd = null, skillSaveError = null, analysis = null,
                    selectedCv = saved.takeIf { cv -> cv.id == id } ?: latest) }
                val notice = if (duplicate) R.string.gap_skill_exists else R.string.gap_skill_saved
                val expectedLevel = if (duplicate) latest.skills.first {
                    it.name.trim().equals(name.trim(), ignoreCase = true)
                }.level else level
                retryAction = { refreshSavedCv(id, offer, name, expectedLevel, notice) }
                refreshAndAnalyze(id, offer, name, expectedLevel, notice)
            } finally { skillUpdateInProgress = false }
        }
    }

    private fun refreshSavedCv(id: String, offer: JobOffer, name: String, level: String, @StringRes notice: Int) {
        runOperation(R.string.job_gap_error_generic, { refreshSavedCv(id, offer, name, level, notice) }) {
            refreshAndAnalyze(id, offer, name, level, notice)
        }
    }

    private suspend fun refreshAndAnalyze(id: String, offer: JobOffer, name: String, level: String, @StringRes notice: Int) {
        val updated = cvs.getCvById(id)
        coroutineContext.ensureActive()
        if (updated.id != id || updated.skills.none {
            it.name.trim().equals(name.trim(), ignoreCase = true) && it.level == level
        }) throw JobSkillsGapException(JobSkillsGapFailure.INCOMPATIBLE_RESPONSE)
        _uiState.update { it.copy(selectedCv = updated, skillNotice = notice,
            cvs = it.cvs.map { cv -> if (cv.id == id) updated else cv }) }
        analyzeResult(updated, offer, lastEvaluationOnly)
    }

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
        resetCourses()
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
        if (skillUpdateInProgress) return
        val state = _uiState.value
        if (state.cvs.none { it.id == id } || (state.isLoading && state.selectedCvId == id)) return
        operation?.cancel()
        resetCourses()
        _uiState.update { it.copy(selectedCvId = id, selectedCv = null, analysis = null,
            skillToAdd = null, skillSaveError = null, skillNotice = null) }
        runOperation(R.string.job_gap_cv_detail_error, { selectCv(id) }) {
            val cv = cvs.getCvById(id)
            coroutineContext.ensureActive()
            if (cv.id != id) throw JobSkillsGapException(JobSkillsGapFailure.INCOMPATIBLE_RESPONSE)
            _uiState.update { it.copy(selectedCv = cv) }
            if (!_uiState.value.offerLocked && _uiState.value.offerPage == 0) loadOffers()
        }
    }

    fun selectOffer(id: String) {
        if (skillUpdateInProgress) return
        val state = _uiState.value
        if (state.offerLocked || state.selectedCv == null) return
        val offer = state.offers.find { it.id == id } ?: return
        operation?.cancel()
        retryAction = null
        resetCourses()
        _uiState.update { it.copy(offer = offer, analysis = null, isLoading = false,
            skillToAdd = null, skillSaveError = null, skillNotice = null,
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
        lastEvaluationOnly = evaluationOnly
        resetCourses()
        _uiState.update { it.copy(analysis = null) }
        runOperation(R.string.job_gap_error_generic, { analyze(evaluationOnly) }) {
            analyzeResult(cv, offer, evaluationOnly)
        }
    }

    private suspend fun analyzeResult(cv: Cv, offer: JobOffer, evaluationOnly: Boolean) {
        val result = gaps.analyze(cv, offer, evaluationOnly)
        coroutineContext.ensureActive()
        _uiState.update { it.copy(analysis = result) }
        if (!evaluationOnly) {
            // After courses succeed, an explicit retry repeats only the failed evaluation POST.
            retryAction = { retryLevels(cv, offer, result) }
            addLevels(cv, offer, result)
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
