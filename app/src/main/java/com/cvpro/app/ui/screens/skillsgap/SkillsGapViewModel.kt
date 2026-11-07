package com.cvpro.app.ui.screens.skillsgap

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cvpro.app.R
import com.cvpro.app.domain.model.SkillInput
import com.cvpro.app.domain.model.SkillLevel
import com.cvpro.app.domain.model.SkillsGapAnalysis
import com.cvpro.app.domain.model.SkillsGapException
import com.cvpro.app.domain.model.SkillsGapFailure
import com.cvpro.app.domain.model.isValidSkillsGapInput
import com.cvpro.app.domain.repository.SkillsGapRepository
import com.google.gson.JsonParseException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

enum class SkillsInputStage { EDIT, REVIEW }

data class SkillsGapUiState(
    val analysis: SkillsGapAnalysis? = null,
    val inputRequired: Boolean = false,
    val isLoading: Boolean = false,
    val inputStage: SkillsInputStage? = null,
    val draft: List<SkillInput> = listOf(SkillInput("", null)),
    @param:StringRes val errorMessage: Int? = null,
    @param:StringRes val inputError: Int? = null,
)

@HiltViewModel
class SkillsGapViewModel @Inject constructor(
    private val skillsGapRepository: SkillsGapRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SkillsGapUiState())
    val uiState = _uiState.asStateFlow()

    init {
        onPage(1)
    }

    fun onRetryClick() = onPage(1)

    fun onPage(page: Long) {
        if (_uiState.value.isLoading || _uiState.value.inputStage != null) return
        _uiState.update { it.copy(isLoading = true, analysis = null, inputRequired = false, errorMessage = null) }
        viewModelScope.launch { readAnalysis(page) }
    }

    fun openInput() {
        _uiState.update {
            if (it.isLoading || (it.analysis == null && !it.inputRequired)) it
            else it.copy(inputStage = SkillsInputStage.EDIT, inputError = null)
        }
    }

    fun closeInput() {
        _uiState.update { it.copy(inputStage = null, inputError = null) }
    }

    fun addSkill() = editDraft { if (size < 500) this + SkillInput("", null) else this }

    fun removeSkill(index: Int) = editDraft { filterIndexed { row, _ -> row != index } }

    fun changeName(index: Int, name: String) = updateSkill(index) { copy(skillName = name) }

    fun changeLevel(index: Int, level: SkillLevel?) = updateSkill(index) { copy(level = level) }

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
                val message = if (error is IOException) R.string.skills_gap_write_network else error.messageRes()
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
            readAnalysis(page = 1)
        }
    }

    private suspend fun readAnalysis(page: Long) {
        try {
            val analysis = skillsGapRepository.getAnalysis(page)
            _uiState.update { it.copy(analysis = analysis, isLoading = false) }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            val inputRequired = (error as? SkillsGapException)?.failure == SkillsGapFailure.INPUT_REQUIRED
            _uiState.update {
                it.copy(isLoading = false, inputRequired = inputRequired, errorMessage = error.messageRes())
            }
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
}

@StringRes
private fun Exception.messageRes(): Int = when (this) {
    is SkillsGapException -> when (failure) {
        SkillsGapFailure.UNAUTHORIZED -> R.string.skills_gap_unauthorized
        SkillsGapFailure.INPUT_REQUIRED -> R.string.skills_gap_input_required
        SkillsGapFailure.INVALID_INPUT -> R.string.skills_gap_invalid_input
        SkillsGapFailure.UNAVAILABLE -> R.string.skills_gap_unavailable
        SkillsGapFailure.INCOMPATIBLE_RESPONSE -> R.string.skills_gap_incompatible
    }
    is JsonParseException -> R.string.skills_gap_incompatible
    is IOException -> R.string.skills_gap_error_network
    else -> R.string.skills_gap_error_generic
}
