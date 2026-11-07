package com.cvpro.app.ui.screens.cvs

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cvpro.app.R
import com.cvpro.app.domain.model.NonLaborExperience
import com.cvpro.app.domain.model.WorkExperience
import com.cvpro.app.domain.repository.CvAiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import javax.inject.Inject

/**
 * Estado de las ayudas de IA del editor de currículum.
 *
 * @property expandingIndex qué experiencia está redactando la IA ahora, o null.
 * @property expandError el error de "Mejorar con IA": a qué experiencia corresponde y qué decir.
 * @property isTranslating si el traductor de experiencia no laboral está esperando a la IA.
 * @property translateError qué decir si el traductor falló.
 * @property result lo que devolvió la IA, para que el editor lo aplique a su borrador. El editor
 *   avisa con [CvAiViewModel.onResultConsumed] para no aplicarlo dos veces.
 */
data class CvAiUiState(
    val expandingIndex: Int? = null,
    val expandError: ExpandError? = null,
    val isTranslating: Boolean = false,
    @param:StringRes val translateError: Int? = null,
    val result: CvAiResult? = null,
) {
    /** Mientras la IA trabaja no se dejan quitar experiencias: cambiaría la posición de la que espera. */
    val isBusy: Boolean get() = expandingIndex != null || isTranslating
}

data class ExpandError(val index: Int, @param:StringRes val message: Int)

/** Respuestas de la IA, ya listas para meter en el borrador. */
sealed interface CvAiResult {

    /** Descripción nueva para la experiencia [index]. [original] queda para "Deshacer". */
    data class Expanded(val index: Int, val original: String, val description: String) : CvAiResult

    /** Experiencia no laboral ya traducida, para agregar al final de la lista. */
    data class Translated(val experience: WorkExperience) : CvAiResult
}

/**
 * Lo que el editor le puede pedir a la IA. Van juntas para no sumarle cuatro lambdas sueltas
 * a cada pantalla que abre el editor. Por defecto no hacen nada (vistas previas).
 */
data class CvAiActions(
    val onExpand: (index: Int, experience: WorkExperience) -> Unit = { _, _ -> },
    val onTranslate: (NonLaborExperience) -> Unit = {},
    val onResultConsumed: () -> Unit = {},
    val onTranslateClosed: () -> Unit = {},
)

/**
 * Llama a la IA del servicio de CVs desde el editor.
 *
 * El editor trabaja sobre un borrador local, que solo se guarda al tocar "Guardar". Por eso
 * este ViewModel no toca el CV: devuelve el texto en [CvAiUiState.result] y el editor decide
 * dónde ponerlo. Nada de lo que escribe la IA se guarda si el usuario no lo confirma.
 */
@HiltViewModel
class CvAiViewModel @Inject constructor(
    private val repository: CvAiRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CvAiUiState())
    val uiState: StateFlow<CvAiUiState> = _uiState.asStateFlow()

    private var job: Job? = null

    /** "Mejorar con IA" sobre la experiencia en la posición [index] del borrador. */
    fun expand(index: Int, experience: WorkExperience) {
        if (_uiState.value.isBusy || experience.description.isBlank()) return
        _uiState.update { it.copy(expandingIndex = index, expandError = null) }
        job = viewModelScope.launch {
            try {
                val description = repository.expandExperience(experience)
                _uiState.update {
                    it.copy(
                        expandingIndex = null,
                        result = CvAiResult.Expanded(index, experience.description, description),
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(expandingIndex = null, expandError = ExpandError(index, exception.toMessage()))
                }
            }
        }
    }

    /** Traductor de experiencia no laboral. Si sale bien, la experiencia va al final de la lista. */
    fun translate(experience: NonLaborExperience) {
        if (_uiState.value.isBusy) return
        _uiState.update { it.copy(isTranslating = true, translateError = null) }
        job = viewModelScope.launch {
            try {
                val translated = repository.translateExperience(experience)
                val nueva = WorkExperience(
                    id = "",
                    company = experience.organization.trim(),
                    position = translated.position,
                    startDate = null,
                    endDate = null,
                    current = false,
                    description = translated.description,
                    // Queda marcada como voluntariado, proyecto escolar o trabajo informal, así el
                    // PDF puede decirlo si la plantilla lo pide.
                    type = experience.type.apiValue,
                )
                _uiState.update { it.copy(isTranslating = false, result = CvAiResult.Translated(nueva)) }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.update { it.copy(isTranslating = false, translateError = exception.toMessage()) }
            }
        }
    }

    /** El editor ya aplicó [CvAiUiState.result]. */
    fun onResultConsumed() {
        _uiState.update { it.copy(result = null) }
    }

    /** Se cerró el traductor: si la IA seguía trabajando, su respuesta ya no hace falta. */
    fun onTranslateClosed() {
        if (_uiState.value.isTranslating) job?.cancel()
        _uiState.update { it.copy(isTranslating = false, translateError = null) }
    }

    /**
     * Vuelve a cero al abrir o cerrar el editor. Sin esto, una respuesta que llegó con el
     * editor cerrado se aplicaría al próximo CV que se abra.
     */
    fun reset() {
        job?.cancel()
        _uiState.value = CvAiUiState()
    }

    /** Un mensaje que el usuario entienda, según por qué falló. */
    @StringRes
    private fun Exception.toMessage(): Int = when {
        this is HttpException && code() == HTTP_BAD_REQUEST -> R.string.ai_error_invalid
        // El servicio corta a los 30 s si Gemini no contesta y responde 503 (confirmado por Pérez):
        // para el usuario es lo mismo que si la app se cansara de esperar.
        this is HttpException && code() == HTTP_SERVICE_UNAVAILABLE -> R.string.ai_error_timeout
        // 404/5xx: el servicio de IA todavía no está desplegado o se cayó.
        this is HttpException -> R.string.ai_error_unavailable
        this is SocketTimeoutException -> R.string.ai_error_timeout
        this is IOException -> R.string.ai_error_network
        else -> R.string.ai_error_generic
    }

    private companion object {
        const val HTTP_BAD_REQUEST = 400
        const val HTTP_SERVICE_UNAVAILABLE = 503
    }
}
