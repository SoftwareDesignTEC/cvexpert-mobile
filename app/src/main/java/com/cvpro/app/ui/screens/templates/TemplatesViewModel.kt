package com.cvpro.app.ui.screens.templates

import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cvpro.app.R
import com.cvpro.app.data.download.TemplatePreviewDownloader
import com.cvpro.app.domain.model.CvTemplate
import com.cvpro.app.domain.model.UserProfile
import com.cvpro.app.domain.repository.AuthRepository
import com.cvpro.app.domain.repository.CvRepository
import com.cvpro.app.domain.repository.ProfileRepository
import com.cvpro.app.domain.repository.TemplateRepository
import com.cvpro.app.ui.components.UiMessage
import com.cvpro.app.ui.components.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TemplatesUiState(
    val templates: List<CvTemplate> = emptyList(),
    val isLoading: Boolean = false,
    @param:StringRes val error: Int? = null,
    /** Plantilla que se está descargando o usando, para apagar su botón mientras tanto. */
    val busyTemplateId: String? = null,
    /** Aviso del resultado de la última acción: se muestra y se limpia. */
    val message: UiMessage? = null,
)

@HiltViewModel
class TemplatesViewModel @Inject constructor(
    private val repository: TemplateRepository,
    private val cvRepository: CvRepository,
    private val previewDownloader: TemplatePreviewDownloader,
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TemplatesUiState())
    val uiState: StateFlow<TemplatesUiState> = _uiState.asStateFlow()

    /** Dónde quedó la última vista previa descargada. La pantalla la abre y la limpia. */
    private val _downloadedPreview = MutableStateFlow<Uri?>(null)
    val downloadedPreview: StateFlow<Uri?> = _downloadedPreview.asStateFlow()

    init {
        loadTemplates()
    }

    fun retry() {
        loadTemplates()
    }

    /**
     * Crea un currículum nuevo con esta plantilla.
     *
     * Va con POST contra el servicio, igual que el resto del CRUD. Nace con los datos personales
     * del perfil (el contrato exige `personalInfo` con nombre y correo) y sin experiencias: el
     * resto se llena después desde "Mis Currículums", con el editor.
     */
    fun onUseTemplate(template: CvTemplate) {
        viewModelScope.launch {
            _uiState.update { it.copy(busyTemplateId = template.id, error = null) }

            try {
                val cv = cvRepository.createCv(
                    title = template.name,
                    templateId = template.id,
                    personalInfo = personalInfoForNewCv(),
                )

                _uiState.update {
                    it.copy(
                        busyTemplateId = null,
                        message = UiMessage(R.string.templates_cv_created, listOf(cv.title)),
                    )
                }
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        busyTemplateId = null,
                        message = UiMessage(exception.toUserMessage()),
                    )
                }
            }
        }
    }

    /**
     * Datos personales con que nace un CV: los del perfil guardado. Si el perfil no se puede
     * leer o le falta el nombre o el correo, se completan con los de la cuenta de Google, que
     * siempre están. Sin sesión devuelve null y el servicio responde 400, que se avisa igual.
     */
    private suspend fun personalInfoForNewCv(): UserProfile? {
        val user = authRepository.currentUser.value ?: return null
        val profile = try {
            profileRepository.getProfile(user.id)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            null
        }
        return profile?.copy(
            name = profile.name.ifBlank { user.name },
            email = profile.email.ifBlank { user.email },
        ) ?: UserProfile(
            id = user.id,
            name = user.name,
            email = user.email,
            phone = null,
            location = "",
            photoUrl = user.photoUrl,
            bio = null,
            types = emptyList(),
            modalities = emptyList(),
            sectors = emptyList(),
        )
    }

    /**
     * Descarga la vista previa de la plantilla a la carpeta de Descargas.
     *
     * Ojo con qué se descarga: el contrato no tiene endpoint que entregue el archivo de la
     * plantilla, así que lo único descargable es la miniatura.
     */
    fun onDownloadTemplate(template: CvTemplate) {
        viewModelScope.launch {
            _uiState.update { it.copy(busyTemplateId = template.id, error = null) }

            try {
                _downloadedPreview.value = previewDownloader.download(
                    templateName = template.name,
                    previewUrl = template.previewUrl,
                )

                _uiState.update {
                    it.copy(
                        busyTemplateId = null,
                        message = UiMessage(R.string.templates_preview_saved),
                    )
                }
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        busyTemplateId = null,
                        message = UiMessage(exception.toUserMessage()),
                    )
                }
            }
        }
    }

    /** La pantalla avisa que ya abrió el archivo, para no volver a abrirlo al repintarse. */
    fun onPreviewOpened() {
        _downloadedPreview.value = null
    }

    /** La pantalla avisa que ya mostró el aviso. */
    fun onMessageShown() {
        _uiState.update { it.copy(message = null) }
    }

    private fun loadTemplates() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                )
            }

            try {
                val result = repository.getTemplates(
                    page = 1,
                    pageSize = 50,
                    query = "",
                )

                _uiState.update {
                    it.copy(
                        templates = result.items,
                        isLoading = false,
                        error = null,
                    )
                }
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        templates = emptyList(),
                        isLoading = false,
                        error = exception.toUserMessage(),
                    )
                }
            }
        }
    }
}
