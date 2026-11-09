package com.cvpro.app.ui.screens.cvs

import android.graphics.Bitmap
import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cvpro.app.data.pdf.CvPdfGenerator
import com.cvpro.app.data.pdf.CvPreviewRenderer
import com.cvpro.app.domain.model.Cv
import com.cvpro.app.domain.repository.CvRepository
import com.cvpro.app.domain.repository.TemplateRepository
import com.cvpro.app.domain.repository.layoutFor
import com.cvpro.app.domain.model.titleToSave
import com.cvpro.app.ui.components.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
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
    /** Primera hoja de cada CV ya dibujada, por id. Las que faltan usan el dibujo simple. */
    val previews: Map<String, Bitmap> = emptyMap(),
)

@HiltViewModel
class CvsViewModel @Inject constructor(
    private val repository: CvRepository,
    private val pdfGenerator: CvPdfGenerator,
    private val templateRepository: TemplateRepository,
    private val pendingCvEditor: PendingCvEditor,
    private val previewRenderer: CvPreviewRenderer,
) : ViewModel() {

    /** Currículum que hay que abrir en el editor (recién creado desde Plantillas). */
    private val _cvToOpen = MutableStateFlow<Cv?>(null)
    val cvToOpen: StateFlow<Cv?> = _cvToOpen.asStateFlow()

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

    /**
     * Se llama cada vez que la pantalla aparece, no solo la primera.
     *
     * El menú guarda el estado de cada pantalla al salir y lo recupera al volver, así que el
     * ViewModel sobrevive. Si cargara solo al crearse, un CV recién creado desde Plantillas
     * no aparecería en la lista. El indicador de carga solo sale si todavía no hay nada que
     * mostrar, para que la lista no parpadee cada vez que se entra.
     */
    fun onScreenShown() {
        loadCvs(showLoader = _uiState.value.cvs.isEmpty())
        val id = pendingCvEditor.take() ?: return
        viewModelScope.launch {
            // Se pide por id y no se busca en la lista: el CV nuevo puede no estar en la
            // primera página. Si falla, el usuario igual lo ve en la lista.
            runCatching { repository.getCvById(id) }.onSuccess { _cvToOpen.value = it }
        }
    }

    /** La pantalla avisa que ya abrió el editor, para no abrirlo otra vez al repintarse. */
    fun onCvOpened() {
        _cvToOpen.value = null
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
                val saved = repository.updateCv(cv.copy(title = titleFor(cv)))

                _uiState.update { state ->
                    state.copy(
                        cvs = state.cvs.map { if (it.id == saved.id) saved else it },
                        isSaving = false,
                    )
                }
                _savedCv.value = saved
                refreshPreviews()
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

    /**
     * Título con que se guarda: si el usuario no escribió uno propio, el automático
     * "Nombre – puesto" con lo que tiene ahora el CV (ver CvTitle.kt).
     */
    private suspend fun titleFor(cv: Cv): String {
        val original = _uiState.value.cvs.firstOrNull { it.id == cv.id } ?: return cv.title
        // El nombre de la plantilla hace falta para reconocer los CV viejos, que se crearon
        // con ese título. Si no se puede pedir, se guarda el título tal cual.
        val templateName = try {
            templateRepository.getTemplate(cv.templateId)?.name.orEmpty()
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            return cv.title
        }
        return cv.titleToSave(original, templateName)
    }

    /** Dibuja la vista previa de los CV de la lista (las ya dibujadas salen del caché). */
    private suspend fun refreshPreviews() {
        val previews = previewRenderer.previewsFor(_uiState.value.cvs)
        _uiState.update { it.copy(previews = previews) }
    }

    /** Avisa que el guardado terminó bien, para que la pantalla cierre el editor. */
    private val _savedCv = MutableStateFlow<Cv?>(null)
    val savedCv: StateFlow<Cv?> = _savedCv.asStateFlow()

    fun onSaveHandled() {
        _savedCv.value = null
    }

    private fun loadCvs(showLoader: Boolean = true) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = showLoader,
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
                refreshPreviews()
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