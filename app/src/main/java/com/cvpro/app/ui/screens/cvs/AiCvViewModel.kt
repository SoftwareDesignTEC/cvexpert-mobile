package com.cvpro.app.ui.screens.cvs

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cvpro.app.R
import com.cvpro.app.domain.model.CvTemplate
import com.cvpro.app.domain.model.newCvTitle
import com.cvpro.app.domain.model.personName
import com.cvpro.app.domain.repository.AuthRepository
import com.cvpro.app.domain.repository.CvAiRepository
import com.cvpro.app.domain.repository.ProfileRepository
import com.cvpro.app.domain.repository.TemplateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import java.net.SocketTimeoutException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

/**
 * Estado del Modo IA: el formulario donde el usuario cuenta su experiencia con sus palabras.
 *
 * @property freeText lo que el usuario escribió o dictó con el micrófono del teclado.
 * @property created true cuando el CV ya se creó: la pantalla lo abre en el editor.
 */
data class AiCvUiState(
    val fullName: String = "",
    val phone: String = "",
    val freeText: String = "",
    val templates: List<CvTemplate> = emptyList(),
    val templateId: String? = null,
    val isGenerating: Boolean = false,
    val showErrors: Boolean = false,
    @param:StringRes val error: Int? = null,
    val created: Boolean = false,
) {
    val nameMissing: Boolean get() = fullName.isBlank()

    /** Con muy poco texto la IA no tiene de dónde sacar un CV; se pide un mínimo. */
    val textTooShort: Boolean get() = freeText.trim().length < MIN_FREE_TEXT

    companion object {
        const val MIN_FREE_TEXT = 40
    }
}

/**
 * Modo IA (POST /cvs/ai/generate-cv): el servicio de CVs arma y guarda un currículum completo
 * a partir de lo que el usuario cuenta. La app solo junta los datos, espera la respuesta y
 * abre el CV creado en el editor para que lo revise.
 *
 * El nombre y el teléfono salen del perfil; el correo no se manda, el servicio lo toma del
 * token (X-User-Email). La plantilla se elige de la lista; si no, la primera.
 */
@HiltViewModel
class AiCvViewModel @Inject constructor(
    private val aiRepository: CvAiRepository,
    private val templateRepository: TemplateRepository,
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val pendingCvEditor: PendingCvEditor,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiCvUiState())
    val uiState: StateFlow<AiCvUiState> = _uiState.asStateFlow()

    /** Llena nombre, teléfono y plantillas. Se llama al abrir la hoja. */
    fun onOpened() {
        _uiState.value = AiCvUiState()
        val user = authRepository.currentUser.value
        _uiState.update { it.copy(fullName = personName(user?.name)) }
        viewModelScope.launch {
            // Si el perfil no se puede leer, quedan el nombre de Google y el teléfono vacío.
            val profile = user?.let { safely { profileRepository.getProfile(it.id) } }
            if (profile != null) {
                _uiState.update {
                    it.copy(
                        fullName = personName(profile.name).ifBlank { it.fullName },
                        phone = profile.phone.orEmpty(),
                    )
                }
            }
        }
        viewModelScope.launch {
            val templates = safely { templateRepository.getTemplates(1, TEMPLATES_PAGE_SIZE, "").items }.orEmpty()
            _uiState.update { it.copy(templates = templates, templateId = templates.firstOrNull()?.id) }
        }
    }

    fun onFullNameChange(value: String) = _uiState.update { it.copy(fullName = value) }

    fun onPhoneChange(value: String) = _uiState.update { it.copy(phone = value) }

    fun onFreeTextChange(value: String) = _uiState.update { it.copy(freeText = value) }

    fun onTemplateChange(id: String) = _uiState.update { it.copy(templateId = id) }

    fun generate() {
        val state = _uiState.value
        if (state.isGenerating) return
        if (state.nameMissing || state.textTooShort) {
            _uiState.update { it.copy(showErrors = true) }
            return
        }
        val template = state.templates.firstOrNull { it.id == state.templateId }
        val templateId = template?.id ?: DEFAULT_TEMPLATE_ID
        _uiState.update { it.copy(isGenerating = true, error = null) }
        viewModelScope.launch {
            try {
                val cv = aiRepository.generateCv(
                    // "Ana Mora – Minimal": al guardar en el editor pasa a "Nombre – puesto".
                    title = newCvTitle(state.fullName, template?.name ?: DEFAULT_TEMPLATE_NAME),
                    templateId = templateId,
                    fullName = state.fullName,
                    phone = state.phone,
                    freeText = state.freeText,
                )
                // Mis Currículums lo abre en el editor apenas se muestre.
                pendingCvEditor.request(cv.id)
                _uiState.update { it.copy(isGenerating = false, created = true) }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.update { it.copy(isGenerating = false, error = exception.toMessage()) }
            }
        }
    }

    /** La pantalla ya abrió el CV creado. */
    fun onCreatedHandled() = _uiState.update { it.copy(created = false) }

    private suspend fun <T> safely(block: suspend () -> T): T? =
        try {
            block()
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            null
        }

    @StringRes
    private fun Exception.toMessage(): Int = when {
        this is HttpException && code() == HTTP_BAD_REQUEST -> R.string.ai_error_invalid
        this is HttpException && code() == HTTP_SERVICE_UNAVAILABLE -> R.string.ai_error_timeout
        this is HttpException -> R.string.ai_error_unavailable
        this is SocketTimeoutException -> R.string.ai_error_timeout
        this is IOException -> R.string.ai_error_network
        else -> R.string.ai_error_generic
    }

    private companion object {
        const val TEMPLATES_PAGE_SIZE = 50
        /** La que usa el servicio si no se elige ninguna (contrato de generate-cv). */
        const val DEFAULT_TEMPLATE_ID = "minimal"
        const val DEFAULT_TEMPLATE_NAME = "Minimal"
        const val HTTP_BAD_REQUEST = 400
        const val HTTP_SERVICE_UNAVAILABLE = 503
    }
}
