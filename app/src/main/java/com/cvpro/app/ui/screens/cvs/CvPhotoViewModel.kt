package com.cvpro.app.ui.screens.cvs

import android.graphics.Bitmap
import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cvpro.app.R
import com.cvpro.app.data.pdf.CvPhotoLoader
import com.cvpro.app.data.pdf.CvPreviewRenderer
import com.cvpro.app.domain.repository.CvRepository
import com.cvpro.app.ui.components.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

/** En qué está la foto: quieta, recortándose o subiéndose. */
enum class PhotoPhase { IDLE, PREPARING, UPLOADING }

/**
 * Estado de la foto en el editor del currículum.
 *
 * @property usesPhoto si la plantilla dibuja la foto. Si no, el editor ni la ofrece.
 * @property preview la foto actual, ya lista para mostrar (venga incrustada o por enlace).
 * @property error por qué no se pudo subir; se borra al intentar de nuevo.
 */
data class CvPhotoUiState(
    val usesPhoto: Boolean = false,
    val preview: Bitmap? = null,
    val phase: PhotoPhase = PhotoPhase.IDLE,
    @param:StringRes val error: Int? = null,
) {
    val isBusy: Boolean get() = phase != PhotoPhase.IDLE
}

/**
 * La foto del currículum: decide si se ofrece, la muestra y sube una nueva.
 *
 * Subir no la guarda en el CV: devuelve el enlace por [uploaded] y el editor lo pone en el
 * borrador. Recién se guarda con "Guardar", igual que cualquier otro campo. Así, si el usuario
 * sale sin guardar, el CV queda como estaba.
 */
@HiltViewModel
class CvPhotoViewModel @Inject constructor(
    private val renderer: CvPreviewRenderer,
    private val photoLoader: CvPhotoLoader,
    private val cvRepository: CvRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CvPhotoUiState())
    val uiState: StateFlow<CvPhotoUiState> = _uiState.asStateFlow()

    private val _uploaded = Channel<String>()

    /** El enlace de cada foto recién subida, para ponerlo en `personalInfo.photoUrl`. */
    val uploaded: Flow<String> = _uploaded.receiveAsFlow()

    private var templateId: String? = null
    private var photoUrl: String? = null
    private var previewJob: Job? = null

    /** Lo llama el editor con la plantilla y la foto del borrador cada vez que cambian. */
    fun bind(templateId: String, photoUrl: String?) {
        if (templateId != this.templateId) {
            this.templateId = templateId
            viewModelScope.launch {
                val usesPhoto = runCatching { renderer.usesPhoto(templateId) }.getOrDefault(false)
                _uiState.update { it.copy(usesPhoto = usesPhoto) }
            }
        }
        if (photoUrl != this.photoUrl) {
            this.photoUrl = photoUrl
            previewJob?.cancel()
            _uiState.update { it.copy(preview = null) }
            if (photoUrl.isNullOrBlank()) return
            previewJob = viewModelScope.launch {
                val preview = photoLoader.load(photoUrl)
                _uiState.update { it.copy(preview = preview) }
            }
        }
    }

    /** El usuario eligió una foto con el selector de Android: se recorta y se sube. */
    fun onPhotoPicked(uri: Uri) {
        if (_uiState.value.isBusy) return
        _uiState.update { it.copy(phase = PhotoPhase.PREPARING, error = null) }
        viewModelScope.launch {
            try {
                val jpeg = photoLoader.prepareForUpload(uri)
                if (jpeg == null) {
                    _uiState.update { it.copy(phase = PhotoPhase.IDLE, error = R.string.cv_photo_error_format) }
                    return@launch
                }
                _uiState.update { it.copy(phase = PhotoPhase.UPLOADING) }
                val url = cvRepository.uploadPhoto(jpeg)
                // Se guarda en memoria lo que ya se tiene, para no descargarlo enseguida.
                photoLoader.remember(url, jpeg)
                _uiState.update { it.copy(phase = PhotoPhase.IDLE) }
                _uploaded.send(url)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.update { it.copy(phase = PhotoPhase.IDLE, error = exception.photoMessage()) }
            }
        }
    }

    /** 413 y 415 tienen su propio aviso; el resto, el mismo de siempre. */
    @StringRes
    private fun Exception.photoMessage(): Int = when {
        this is HttpException && code() == HTTP_PAYLOAD_TOO_LARGE -> R.string.cv_photo_error_size
        this is HttpException && code() == HTTP_UNSUPPORTED_MEDIA_TYPE -> R.string.cv_photo_error_format
        else -> toUserMessage()
    }

    private companion object {
        const val HTTP_PAYLOAD_TOO_LARGE = 413
        const val HTTP_UNSUPPORTED_MEDIA_TYPE = 415
    }
}
