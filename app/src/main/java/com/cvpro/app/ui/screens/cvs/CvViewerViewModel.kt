package com.cvpro.app.ui.screens.cvs

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cvpro.app.data.pdf.CvPreviewRenderer
import com.cvpro.app.domain.model.Cv
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Estado de "Ver currículum".
 *
 * @property pages las hojas del CV dibujadas como en el PDF; vacía mientras carga.
 * @property failed no se pudieron dibujar (no debería pasar: se dibuja en el teléfono).
 */
data class CvViewerUiState(
    val isLoading: Boolean = true,
    val pages: List<Bitmap> = emptyList(),
    val failed: Boolean = false,
)

/** Dibuja las hojas del currículum para mostrarlas como el documento descargado. */
@HiltViewModel
class CvViewerViewModel @Inject constructor(
    private val renderer: CvPreviewRenderer,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CvViewerUiState())
    val uiState: StateFlow<CvViewerUiState> = _uiState.asStateFlow()

    private var shownCv: Cv? = null

    /** Dibuja [cv]. Si ya es el que se muestra, no hace nada (al rotar o recomponer). */
    fun show(cv: Cv) {
        if (cv == shownCv && !_uiState.value.failed) return
        shownCv = cv
        _uiState.value = CvViewerUiState()
        viewModelScope.launch {
            _uiState.value = try {
                CvViewerUiState(isLoading = false, pages = renderer.pagesFor(cv))
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                CvViewerUiState(isLoading = false, failed = true)
            }
        }
    }
}
