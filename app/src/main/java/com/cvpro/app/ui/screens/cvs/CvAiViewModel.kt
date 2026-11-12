package com.cvpro.app.ui.screens.cvs

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cvpro.app.R
import com.cvpro.app.domain.model.AiTextField
import com.cvpro.app.domain.model.Cv
import com.cvpro.app.domain.model.NonLaborExperience
import com.cvpro.app.domain.model.WorkExperience
import com.cvpro.app.domain.repository.CvAiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import java.net.SocketTimeoutException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

/**
 * Estado de las ayudas de IA del editor de currículum.
 *
 * @property expandingIndex qué experiencia está redactando la IA ahora, o null.
 * @property expandError el error de "Mejorar con IA": a qué experiencia corresponde y qué decir.
 * @property isTranslating si el traductor de experiencia no laboral está esperando a la IA.
 * @property translateError qué decir si el traductor falló.
 * @property isGeneratingBio si "Generar con IA" del perfil está esperando a la IA.
 * @property bioError qué decir si la IA no pudo escribir el perfil.
 * @property result lo que devolvió la IA, para que el editor lo aplique a su borrador. El editor
 *   avisa con [CvAiViewModel.onResultConsumed] para no aplicarlo dos veces.
 */
data class CvAiUiState(
    val expandingIndex: Int? = null,
    val expandError: ExpandError? = null,
    val isTranslating: Boolean = false,
    @param:StringRes val translateError: Int? = null,
    val isGeneratingBio: Boolean = false,
    @param:StringRes val bioError: Int? = null,
    val result: CvAiResult? = null,
) {
    /** Mientras la IA trabaja no se dejan quitar experiencias: cambiaría la posición de la que espera. */
    val isBusy: Boolean get() = expandingIndex != null || isTranslating || isGeneratingBio
}

data class ExpandError(val index: Int, @param:StringRes val message: Int)

/** Respuestas de la IA, ya listas para meter en el borrador. */
sealed interface CvAiResult {

    /** Descripción nueva para la experiencia [index]. [original] queda para "Deshacer". */
    data class Expanded(val index: Int, val original: String, val description: String) : CvAiResult

    /** Experiencia no laboral ya traducida, para agregar al final de la lista. */
    data class Translated(val experience: WorkExperience) : CvAiResult

    /** Perfil profesional escrito por la IA. [original] queda para "Deshacer". */
    data class GeneratedBio(val original: String, val bio: String) : CvAiResult
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
    /** "Generar con IA" del perfil: recibe el borrador entero para usarlo de contexto. */
    val onGenerateBio: (Cv) -> Unit = {},
    /** "Generar con IA" de una descripción vacía, a partir del puesto y la empresa. */
    val onGenerateDescription: (index: Int, experience: WorkExperience) -> Unit = { _, _ -> },
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

    /**
     * "Generar con IA" de una descripción que está vacía: la IA la escribe desde cero con el
     * puesto, la empresa y el tipo de experiencia. Llega como [CvAiResult.Expanded], así el
     * editor la aplica y ofrece "Deshacer" igual que con "Mejorar con IA".
     */
    fun generateDescription(index: Int, experience: WorkExperience) {
        if (_uiState.value.isBusy || experience.position.isBlank()) return
        _uiState.update { it.copy(expandingIndex = index, expandError = null) }
        job = viewModelScope.launch {
            try {
                val text = repository.generateText(AiTextField.EXPERIENCE_DESCRIPTION, descriptionContext(experience))
                _uiState.update {
                    it.copy(
                        expandingIndex = null,
                        result = CvAiResult.Expanded(index, experience.description, text),
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

    /** "Generar con IA" del perfil profesional, con todo lo que ya tiene el CV como contexto. */
    fun generateBio(cv: Cv) {
        if (_uiState.value.isBusy) return
        _uiState.update { it.copy(isGeneratingBio = true, bioError = null) }
        job = viewModelScope.launch {
            try {
                val bio = repository.generateText(AiTextField.PROFILE_BIO, bioContext(cv))
                _uiState.update {
                    it.copy(
                        isGeneratingBio = false,
                        result = CvAiResult.GeneratedBio(cv.personalInfo.bio.orEmpty(), bio),
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.update { it.copy(isGeneratingBio = false, bioError = exception.toMessage()) }
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

    /**
     * Contexto para escribir una descripción: lo que el usuario ya puso. El servicio no exige un
     * formato; son líneas "Etiqueta: valor" que el modelo entiende sin problema.
     */
    private fun descriptionContext(experience: WorkExperience): String = listOfNotNull(
        "Puesto: ${experience.position.trim()}",
        experience.company.trim().takeIf { it.isNotEmpty() }?.let { "Empresa u organización: $it" },
        "Tipo de experiencia: ${experience.type.ifBlank { "formal" }}",
        experience.description.trim().takeIf { it.isNotEmpty() }?.let { "Lo que escribió la persona: $it" },
    ).joinToString("\n")

    /** Contexto para el perfil: nombre, experiencias, estudios, habilidades y el perfil actual. */
    private fun bioContext(cv: Cv): String = listOfNotNull(
        cv.personalInfo.name.trim().takeIf { it.isNotEmpty() }?.let { "Nombre: $it" },
        cv.workExperiences.filter { it.position.isNotBlank() }.takeIf { it.isNotEmpty() }
            ?.joinToString("; ") { e -> listOf(e.position, e.company).filter { it.isNotBlank() }.joinToString(" en ") }
            ?.let { "Experiencias: $it" },
        cv.education.filter { it.degree.isNotBlank() }.takeIf { it.isNotEmpty() }
            ?.joinToString("; ") { e -> listOf(e.degree, e.institution).filter { it.isNotBlank() }.joinToString(", ") }
            ?.let { "Estudios: $it" },
        cv.skills.map { it.name }.filter { it.isNotBlank() }.takeIf { it.isNotEmpty() }
            ?.let { "Habilidades: ${it.joinToString(", ")}" },
        cv.languages.map { it.name }.filter { it.isNotBlank() }.takeIf { it.isNotEmpty() }
            ?.let { "Idiomas: ${it.joinToString(", ")}" },
        cv.personalInfo.bio?.trim()?.takeIf { it.isNotEmpty() }?.let { "Perfil actual: $it" },
    ).joinToString("\n")

    private companion object {
        const val HTTP_BAD_REQUEST = 400
        const val HTTP_SERVICE_UNAVAILABLE = 503
    }
}
