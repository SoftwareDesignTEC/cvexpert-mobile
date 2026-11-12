package com.cvpro.app.ui.screens.skillsgap

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cvpro.app.R
import com.cvpro.app.data.mapper.toSkillsGapInput
import com.cvpro.app.domain.model.*
import com.cvpro.app.domain.repository.CvRepository
import com.cvpro.app.domain.repository.SkillsGapRepository
import com.google.gson.JsonParseException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

enum class SkillsInputStage { EDIT, REVIEW }

data class SkillsGapUiState(
    val enabled: Boolean = false,
    val analysis: SkillsGapAnalysis? = null,
    val inputRequired: Boolean = false,
    val inputStage: SkillsInputStage? = null,
    val draft: List<SkillInput> = listOf(SkillInput("", null)),
    @param:StringRes val inputError: Int? = null,
    val isLoading: Boolean = false,
    @param:StringRes val errorMessage: Int? = null,
    @param:StringRes val cvErrorMessage: Int? = null,
    val cvs: List<Cv> = emptyList(),
    val cvPage: Int = 0,
    val hasMoreCvs: Boolean = false,
    val selectedCv: Cv? = null,
    val input: List<SkillInput>? = null,
)

@HiltViewModel
class SkillsGapViewModel @Inject constructor(
    private val skillsGapRepository: SkillsGapRepository,
    private val cvRepository: CvRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SkillsGapUiState(enabled = skillsGapRepository.enabled))
    val uiState = _uiState.asStateFlow()
    private var job: Job? = null

    init { onRetryClick() }

    // Retry only reads. Replacing the snapshot always requires the explicit button.
    fun onRetryClick() = runOperation {
        if (_uiState.value.cvs.isEmpty()) loadCvs(1)
        val analysis = skillsGapRepository.getAnalysis()
        _uiState.update { it.copy(analysis = analysis, inputRequired = false) }
    }

    fun onMoreCvs() = runOperation { loadCvs(_uiState.value.cvPage + 1) }

    private suspend fun loadCvs(page: Int) {
        _uiState.update { it.copy(cvErrorMessage = null) }
        try {
            val result = cvRepository.getCvs(page, 20, "")
            _uiState.update { it.copy(
                cvs = (if (page == 1) result.items else it.cvs + result.items).distinctBy { cv -> cv.id },
                cvPage = result.page, hasMoreCvs = result.hasMore,
            ) }
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            // Reading an existing analysis does not depend on the CV service.
            _uiState.update { it.copy(cvErrorMessage = skillsGapErrorMessage(e)) }
        }
    }

    fun onSelectCv(id: String) = runOperation {
        _uiState.update { it.copy(selectedCv = null, input = null, analysis = null) }
        val cv = cvRepository.getCvById(id)
        _uiState.update { it.copy(selectedCv = cv, input = cv.skills.toSkillsGapInput()) }
    }

    fun onAnalyzeClick() = runOperation {
        val input = _uiState.value.input ?: return@runOperation
        // A successful PUT replaces the server snapshot even if the subsequent GET fails.
        _uiState.update { it.copy(analysis = null) }
        skillsGapRepository.replaceInput(input)
        val analysis = skillsGapRepository.getAnalysis()
        _uiState.update { it.copy(analysis = analysis) }
    }

    fun onPage(page: Int) = runOperation {
        _uiState.update { it.copy(analysis = null) }
        val analysis = skillsGapRepository.getAnalysis(page)
        _uiState.update { it.copy(analysis = analysis) }
    }

    fun openInput() {
        _uiState.update {
            if (!skillsGapRepository.enabled || it.isLoading || (it.analysis == null && !it.inputRequired)) it
            else it.copy(inputStage = SkillsInputStage.EDIT, inputError = null)
        }
    }

    fun closeInput() {
        _uiState.update { it.copy(inputStage = null, inputError = null) }
    }

    fun addSkill() = editDraft { if (size < 500) this + SkillInput("", null) else this }

    fun removeSkill(index: Int) = editDraft { filterIndexed { row, _ -> row != index } }

    fun changeName(index: Int, name: String) = updateSkill(index) { copy(skillName = name) }

    fun changeLevel(index: Int, level: SkillLevel?) = updateSkill(index) { copy(level = level?.apiValue) }

    fun reviewInput() {
        _uiState.update { state ->
            if (state.inputStage != SkillsInputStage.EDIT) return@update state
            val draft = state.draft.map { it.copy(skillName = it.skillName.trim()) }
            if (draft.isValidSkillsGapInput()) state.copy(draft = draft, inputStage = SkillsInputStage.REVIEW, inputError = null)
            else state.copy(inputError = R.string.skills_gap_invalid_input)
        }
    }

    fun editInput() {
        _uiState.update {
            if (it.inputStage == SkillsInputStage.REVIEW) it.copy(inputStage = SkillsInputStage.EDIT)
            else it
        }
    }

    fun confirmInput() {
        val state = _uiState.value
        if (state.isLoading || state.inputStage != SkillsInputStage.REVIEW) return
        _uiState.value = state.copy(
            isLoading = true, inputStage = null, analysis = null, inputRequired = false, errorMessage = null,
        )
        viewModelScope.launch {
            try {
                skillsGapRepository.replaceInput(state.draft)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                val message = if (error is IOException) R.string.skills_gap_write_network else skillsGapErrorMessage(error)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        inputStage = SkillsInputStage.EDIT,
                        errorMessage = message,
                        inputError = message,
                    )
                }
                return@launch
            }
            try {
                val analysis = skillsGapRepository.getAnalysis(1)
                _uiState.update { it.copy(analysis = analysis, isLoading = false) }
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) { _uiState.update { it.copy(isLoading = false, errorMessage = skillsGapErrorMessage(error)) } }
        }
    }


    private fun updateSkill(index: Int, change: SkillInput.() -> SkillInput) =
        editDraft { mapIndexed { row, skill -> if (row == index) skill.change() else skill } }

    private fun editDraft(change: List<SkillInput>.() -> List<SkillInput>) {
        _uiState.update {
            if (it.inputStage == SkillsInputStage.EDIT) it.copy(draft = it.draft.change(), inputError = null)
            else it
        }
    }
    private fun runOperation(block: suspend () -> Unit) {
        if (!skillsGapRepository.enabled || job?.isActive == true || _uiState.value.isLoading || _uiState.value.inputStage != null) return
        job = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try { block() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                _uiState.update { it.copy(analysis = null, inputRequired = (e as? SkillsGapException)?.failure == SkillsGapFailure.INPUT_REQUIRED, errorMessage = skillsGapErrorMessage(e)) }
            } finally { _uiState.update { it.copy(isLoading = false) } }
        }
    }
}

@StringRes
internal fun skillsGapErrorMessage(error: Exception): Int = when {
    error is SkillsGapException -> when (error.failure) {
        SkillsGapFailure.DISABLED -> R.string.skills_gap_disabled
        SkillsGapFailure.UNAUTHORIZED -> R.string.skills_gap_unauthorized
        SkillsGapFailure.INPUT_REQUIRED -> R.string.skills_gap_input_required
        SkillsGapFailure.INVALID_INPUT -> R.string.skills_gap_invalid_input
        SkillsGapFailure.INCOMPATIBLE_RESPONSE -> R.string.skills_gap_incompatible
        SkillsGapFailure.UNAVAILABLE -> R.string.skills_gap_unavailable
    }
    error is HttpException && error.code() == 401 -> R.string.skills_gap_unauthorized
    error is JsonParseException -> R.string.skills_gap_incompatible
    error is IOException -> R.string.skills_gap_error_network
    else -> R.string.skills_gap_error_generic
}
