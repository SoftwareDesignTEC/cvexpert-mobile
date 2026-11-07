package com.cvpro.app.ui.screens.cvs

import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cvpro.app.data.pdf.CvPdfGenerator
import com.cvpro.app.domain.model.Cv
import com.cvpro.app.domain.repository.CvRepository
import com.cvpro.app.domain.repository.TemplateRepository
import com.cvpro.app.domain.repository.layoutFor
import com.cvpro.app.ui.components.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CvsUiState(
    val cvs: List<Cv> = emptyList(),
    val query: String = "",
    val isLoading: Boolean = false,
    val isGeneratingPdf: Boolean = false,
    @param:StringRes val error: Int? = null,
    val deletingId: String? = null,
    val isSaving: Boolean = false,
)

@HiltViewModel
class CvsViewModel @Inject constructor(
    private val repository: CvRepository,
    private val pdfGenerator: CvPdfGenerator,
    private val templateRepository: TemplateRepository,
) : ViewModel() {

    // Dónde quedó el último PDF generado. La pantalla lo abre y después lo limpia.
    private val _generatedPdf = MutableStateFlow<Uri?>(null)
    val generatedPdf: StateFlow<Uri?> = _generatedPdf.asStateFlow()

    /**
     * Arma el PDF del currículum y lo guarda en Descargas.
     *
     * El dibujo y la escritura del archivo corren fuera del hilo principal, dentro del
     * generador, para no trabar la pantalla mientras tanto.
     */
    fun onDownloadCv(cv: Cv) {
        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingPdf = true, error = null) }
            try {
                _generatedPdf.value = pdfGenerator.generate(cv, templateRepository.layoutFor(cv))
                _uiState.update { it.copy(isGeneratingPdf = false) }
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(isGeneratingPdf = false, error = exception.toUserMessage())
                }
            }
        }
    }

    /** La pantalla avisa que ya abrió el archivo, para no volver a abrirlo al repintarse. */
    fun onPdfOpened() {
        _generatedPdf.value = null
    }

    private val _uiState = MutableStateFlow(CvsUiState())
    val uiState: StateFlow<CvsUiState> = _uiState.asStateFlow()

    /** Espera de la búsqueda, para no pedirle al servicio en cada tecla. */
    private var searchJob: Job? = null

    init {
        loadCvs()
    }

    fun retry() {
        loadCvs()
    }

    /**
     * Texto del buscador.
     *
     * No pide al servicio en cada tecla: cancela la búsqueda anterior y espera un momento a
     * que el usuario deje de escribir.
     */
    fun onQueryChange(value: String) {
        _uiState.update { it.copy(query = value) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(SEARCH_DELAY_MS)
            loadCvs()
        }
    }

    fun deleteCv(id: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    deletingId = id,
                    error = null,
                )
            }

            try {
                repository.deleteCv(id)

                _uiState.update { state ->
                    state.copy(
                        cvs = state.cvs.filterNot { it.id == id },
                        deletingId = null,
                    )
                }
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        deletingId = null,
                        error = exception.toUserMessage(),
                    )
                }
            }
        }
    }

    /**
     * Guarda el currículum editado contra el servicio.
     *
     * La lista se actualiza con lo que devuelve el PUT, no con el borrador: si el servicio
     * cambia algo al guardar, la pantalla muestra lo que realmente quedó almacenado.
     */
    fun saveCv(cv: Cv) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }

            try {
                val saved = repository.updateCv(cv)

                _uiState.update { state ->
                    state.copy(
                        cvs = state.cvs.map { if (it.id == saved.id) saved else it },
                        isSaving = false,
                    )
                }
                _savedCv.value = saved
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = exception.toUserMessage(),
                    )
                }
            }
        }
    }

    /** Avisa que el guardado terminó bien, para que la pantalla cierre el editor. */
    private val _savedCv = MutableStateFlow<Cv?>(null)
    val savedCv: StateFlow<Cv?> = _savedCv.asStateFlow()

    fun onSaveHandled() {
        _savedCv.value = null
    }

    private fun loadCvs() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                )
            }

            try {
                val result = repository.getCvs(
                    page = 1,
                    pageSize = PAGE_SIZE,
                    query = _uiState.value.query.trim(),
                )

                _uiState.update {
                    it.copy(
                        cvs = result.items,
                        isLoading = false,
                        error = null,
                    )
                }
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        cvs = emptyList(),
                        isLoading = false,
                        error = exception.toUserMessage(),
                    )
                }
            }
        }
    }

    private companion object {
        const val PAGE_SIZE = 50
        const val SEARCH_DELAY_MS = 350L
    }
}