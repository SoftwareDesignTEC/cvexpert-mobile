package com.cvpro.app.ui.screens.skillsgap

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cvpro.app.R
import com.cvpro.app.domain.model.SkillsGapAnalysis
import com.cvpro.app.domain.repository.SkillsGapRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

/** Estado de Análisis de Brechas: cargando, error o el análisis listo. */
data class SkillsGapUiState(
    val analysis: SkillsGapAnalysis? = null,
    val isLoading: Boolean = false,
    @param:StringRes val errorMessage: Int? = null,
)

@HiltViewModel
class SkillsGapViewModel @Inject constructor(
    private val skillsGapRepository: SkillsGapRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SkillsGapUiState())
    val uiState: StateFlow<SkillsGapUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun onRetryClick() = load()

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching { skillsGapRepository.getAnalysis() }
                .onSuccess { analysis ->
                    _uiState.update { it.copy(analysis = analysis, isLoading = false) }
                }
                .onFailure { error ->
                    val message = if (error is IOException) {
                        R.string.skills_gap_error_network
                    } else {
                        R.string.skills_gap_error_generic
                    }
                    _uiState.update { it.copy(analysis = null, isLoading = false, errorMessage = message) }
                }
        }
    }
}
