package com.cvpro.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cvpro.app.domain.model.Cv
import com.cvpro.app.domain.model.JobOffer
import com.cvpro.app.ui.screens.cvs.PendingCvEditor
import android.graphics.Bitmap
import android.net.Uri
import com.cvpro.app.data.pdf.CvPdfGenerator
import com.cvpro.app.data.pdf.CvPreviewRenderer
import com.cvpro.app.domain.repository.CvRepository
import com.cvpro.app.domain.repository.TemplateRepository
import com.cvpro.app.domain.repository.layoutFor
import com.cvpro.app.domain.repository.JobOfferRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Estado del Panel Principal.
 *
 * Cada sección del diseño de Samuel tiene su dato. Si una falla no se cae la pantalla
 * entera: esa sección se muestra vacía y las demás siguen.
 */
data class HomeUiState(
    val recentCvs: List<Cv> = emptyList(),
    /** Primera hoja de cada CV reciente ya dibujada, por id. */
    val cvPreviews: Map<String, Bitmap> = emptyMap(),
    val recommendedOffers: List<JobOffer> = emptyList(),
    val isLoading: Boolean = false,
    val isGeneratingPdf: Boolean = false,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val cvRepository: CvRepository,
    private val jobOfferRepository: JobOfferRepository,
    private val pdfGenerator: CvPdfGenerator,
    private val templateRepository: TemplateRepository,
    private val previewRenderer: CvPreviewRenderer,
    private val pendingCvEditor: PendingCvEditor,
) : ViewModel() {

    /** El lápiz de una tarjeta: Mis Currículums abrirá ese CV en el editor. */
    fun onEditCv(cv: Cv) = pendingCvEditor.request(cv.id)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Dónde quedó el último PDF generado. La pantalla lo abre y después lo limpia.
    private val _generatedPdf = MutableStateFlow<Uri?>(null)
    val generatedPdf: StateFlow<Uri?> = _generatedPdf.asStateFlow()

    /**
     * Se llama cada vez que el panel aparece. El menú restaura el ViewModel al volver, y si
     * cargara solo al crearse no mostraría un CV recién creado ni una postulación nueva.
     */
    fun onScreenShown() = load(showLoader = _uiState.value.recentCvs.isEmpty())

    fun retry() = load()

    /**
     * Arma el PDF del currículum y lo guarda en Descargas.
     *
     * Es el mismo generador que usa Mis Currículums: el botón se llama igual en las dos
     * pantallas, así que tiene que hacer lo mismo en las dos.
     */
    fun onDownloadCv(cv: Cv) {
        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingPdf = true) }
            try {
                _generatedPdf.value = pdfGenerator.generate(cv, templateRepository.layoutFor(cv))
            } finally {
                _uiState.update { it.copy(isGeneratingPdf = false) }
            }
        }
    }

    /** La pantalla avisa que ya abrió el archivo, para no volver a abrirlo al repintarse. */
    fun onPdfOpened() {
        _generatedPdf.value = null
    }

    /**
     * Carga CVs y ofertas en paralelo.
     *
     * Con `async` las llamadas salen en paralelo en vez de una tras otra, así el panel
     * tarda lo que la más lenta y no la suma de ambas.
     */
    private fun load(showLoader: Boolean = true) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = showLoader) }

            val cvs = async { runCatching { cvRepository.getCvs(1, SECTION_SIZE, "").items } }
            val offers = async { runCatching { jobOfferRepository.getOffers(1, SECTION_SIZE, "").items } }

            _uiState.update {
                it.copy(
                    recentCvs = cvs.await().getOrDefault(emptyList()),
                    recommendedOffers = offers.await().getOrDefault(emptyList()),
                    isLoading = false,
                )
            }

            val previews = previewRenderer.previewsFor(_uiState.value.recentCvs)
            _uiState.update { it.copy(cvPreviews = previews) }
        }
    }

    private companion object {
        /** El diseño muestra tres elementos por sección. */
        const val SECTION_SIZE = 3
    }
}
