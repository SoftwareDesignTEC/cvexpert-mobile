package com.cvpro.app.ui.components

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.cvpro.app.R
import com.cvpro.app.domain.model.Cv
import com.cvpro.app.domain.model.CvLevel
import com.cvpro.app.domain.model.Education
import com.cvpro.app.domain.model.Language
import com.cvpro.app.domain.model.LanguageLevel
import com.cvpro.app.domain.model.NonLaborExperience
import com.cvpro.app.domain.model.NonLaborType
import com.cvpro.app.domain.model.Skill
import com.cvpro.app.domain.model.SkillLevel
import com.cvpro.app.domain.model.WorkExperience
import com.cvpro.app.domain.model.toCvDay
import com.cvpro.app.ui.screens.cvs.CvAiActions
import kotlinx.coroutines.launch
import com.cvpro.app.ui.screens.cvs.CvAiResult
import com.cvpro.app.ui.screens.cvs.CvAiUiState

/**
 * Editor completo de un currículum.
 *
 * Trabaja sobre un borrador: todo lo que el usuario escribe cambia una copia local del
 * currículum, y recién al tocar "Guardar" se manda al servicio con `PUT /cvs/{id}`. Si
 * cierra la hoja sin guardar, el borrador se descarta y el original queda intacto.
 *
 * Las secciones de experiencia, educación, habilidades e idiomas son listas: se puede
 * agregar una entrada vacía al final o quitar cualquiera. Las entradas nuevas van sin id;
 * el servicio es el que asigna uno al guardarlas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CvEditorSheet(
    cv: Cv,
    isSaving: Boolean,
    onSave: (Cv) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    aiState: CvAiUiState = CvAiUiState(),
    aiActions: CvAiActions = CvAiActions(),
) {
    // El borrador arranca del currículum recibido y se reinicia si llega otro distinto.
    var draft by remember(cv.id) { mutableStateOf(cv) }
    // Descripción que tenía una experiencia antes de que la IA la redactara (posición y texto),
    // para poder deshacerlo. Una sola a la vez: la última que se mejoró.
    var undo by remember(cv.id) { mutableStateOf<Pair<Int, String>?>(null) }
    var showNonLabor by remember(cv.id) { mutableStateOf(false) }
    // Perfil que había antes de que la IA lo escribiera, para "Deshacer".
    var bioUndo by remember(cv.id) { mutableStateOf<String?>(null) }

    // Lo que devuelve la IA entra al borrador como si el usuario lo hubiera escrito: no se
    // guarda hasta que toque "Guardar".
    LaunchedEffect(aiState.result) {
        when (val result = aiState.result) {
            is CvAiResult.Expanded -> {
                val experience = draft.workExperiences.getOrNull(result.index)
                if (experience != null) {
                    draft = draft.copy(
                        workExperiences = draft.workExperiences.replaceAt(
                            result.index,
                            experience.copy(description = result.description),
                        ),
                    )
                    undo = result.index to result.original
                }
            }
            is CvAiResult.Translated -> {
                draft = draft.copy(workExperiences = draft.workExperiences + result.experience)
                showNonLabor = false
            }
            is CvAiResult.GeneratedBio -> {
                draft = draft.copy(personalInfo = draft.personalInfo.copy(bio = result.bio))
                bioUndo = result.original
            }
            null -> return@LaunchedEffect
        }
        aiActions.onResultConsumed()
    }

    if (showNonLabor) {
        NonLaborDialog(
            isTranslating = aiState.isTranslating,
            errorMessage = aiState.translateError,
            onTranslate = aiActions.onTranslate,
            onDismiss = {
                showNonLabor = false
                aiActions.onTranslateClosed()
            },
        )
    }
    // Los errores solo aparecen después del primer intento de guardar, para no pintar el
    // formulario en rojo apenas se abre.
    var showErrors by remember(cv.id) { mutableStateOf(false) }

    val titleError = FormValidation.required(draft.title)
    val emailError = FormValidation.email(draft.personalInfo.email)
    val nameError = FormValidation.required(draft.personalInfo.name)
    val phoneError = FormValidation.phoneOptional(draft.personalInfo.phone.orEmpty())
    // El servicio de CVs exige la fecha de inicio de cada experiencia y de cada estudio
    // (startDate es obligatorio en el contrato). Sin esto el editor dejaba guardar y el
    // servicio respondía 400 con un aviso genérico, sin decir qué faltaba.
    val missingStartDate = draft.workExperiences.any { it.startDate == null } ||
        draft.education.any { it.startDate == null }
    // El contrato de CVs exige puesto y descripción siempre, y empresa solo en empleos formales:
    // en un voluntariado o un proyecto escolar puede no haber empresa.
    val experienceErrors = draft.workExperiences.any { it.missingPosition() || it.missingDescription() || it.missingCompany() }
    val hasErrors = listOf(titleError, emailError, nameError, phoneError).any { it != null } ||
        missingStartDate || experienceErrors || FormValidation.hasInvalidSkills(draft.skills)

    // Cada campo que puede fallar se registra con una clave. Al guardar con errores, la hoja se
    // desplaza sola al primero, para que el usuario no tenga que buscar qué falta.
    val scope = rememberCoroutineScope()
    val requesters = remember(cv.id) { mutableMapOf<String, BringIntoViewRequester>() }
    fun requester(key: String) = requesters.getOrPut(key) { BringIntoViewRequester() }
    fun firstErrorKey(): String? = when {
        titleError != null -> KEY_TITLE
        nameError != null -> KEY_NAME
        emailError != null -> KEY_EMAIL
        phoneError != null -> KEY_PHONE
        else -> draft.workExperiences.withIndex().firstNotNullOfOrNull { (i, e) ->
            when {
                e.missingPosition() -> "$KEY_EXPERIENCE_POSITION$i"
                e.missingCompany() -> "$KEY_EXPERIENCE_COMPANY$i"
                e.startDate == null -> "$KEY_EXPERIENCE$i"
                e.missingDescription() -> "$KEY_EXPERIENCE_DESCRIPTION$i"
                else -> null
            }
        } ?: draft.education.indexOfFirst { it.startDate == null }.takeIf { it >= 0 }
            ?.let { "$KEY_EDUCATION$it" }
    }

    // Cerrar sin querer (un deslizón hacia abajo, tocar fuera o el botón atrás) borraba todo lo
    // escrito. Con cambios sin guardar, la hoja no se cierra: pregunta si se quieren descartar.
    val hasChanges by rememberUpdatedState(draft != cv)
    var confirmDiscard by remember(cv.id) { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        // Lo consulta la hoja antes de cerrarse por cualquiera de esas tres vías; false la deja
        // abierta (vuelve a su lugar si se estaba deslizando).
        confirmValueChange = { value ->
            if (value == SheetValue.Hidden && hasChanges) {
                confirmDiscard = true
                false
            } else {
                true
            }
        },
    )
    fun requestClose() {
        if (hasChanges) confirmDiscard = true else onDismiss()
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text(stringResource(R.string.cv_editor_discard_title)) },
            text = { Text(stringResource(R.string.cv_editor_discard_message)) },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cv_editor_discard_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDiscard = false }) {
                    Text(stringResource(R.string.cv_editor_discard_keep))
                }
            },
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier,
    ) {
        // El botón atrás no pasa por confirmValueChange (Material 3 cierra la hoja directo), así
        // que se intercepta acá. Se registra después que el de la hoja, por eso tiene prioridad.
        BackHandler(enabled = hasChanges) { confirmDiscard = true }
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                text = stringResource(R.string.cv_editor_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )

            FormTextField(
                value = draft.title,
                onValueChange = { draft = draft.copy(title = it) },
                label = stringResource(R.string.cv_editor_cv_title),
                errorMessage = titleError.messageOrNull(showErrors),
                placeholder = stringResource(R.string.cv_example_title),
                modifier = Modifier
                    .padding(top = 12.dp)
                    .bringIntoViewRequester(requester(KEY_TITLE)),
            )

            // ----- Información personal -----
            EditorSection(title = stringResource(R.string.cv_detail_personal))

            FormTextField(
                value = draft.personalInfo.name,
                onValueChange = {
                    draft = draft.copy(personalInfo = draft.personalInfo.copy(name = it))
                },
                label = stringResource(R.string.cv_editor_name),
                errorMessage = nameError.messageOrNull(showErrors),
                placeholder = stringResource(R.string.cv_example_name),
                modifier = Modifier.bringIntoViewRequester(requester(KEY_NAME)),
            )
            FormTextField(
                value = draft.personalInfo.email,
                onValueChange = {
                    draft = draft.copy(personalInfo = draft.personalInfo.copy(email = it))
                },
                label = stringResource(R.string.cv_editor_email),
                errorMessage = emailError.messageOrNull(showErrors),
                keyboardType = KeyboardType.Email,
                placeholder = stringResource(R.string.cv_example_email),
                modifier = Modifier.bringIntoViewRequester(requester(KEY_EMAIL)),
            )
            FormTextField(
                value = draft.personalInfo.phone.orEmpty(),
                onValueChange = {
                    draft = draft.copy(
                        personalInfo = draft.personalInfo.copy(phone = it.ifBlank { null }),
                    )
                },
                label = stringResource(R.string.cv_editor_phone),
                errorMessage = phoneError.messageOrNull(showErrors),
                keyboardType = KeyboardType.Phone,
                placeholder = stringResource(R.string.cv_example_phone),
                modifier = Modifier.bringIntoViewRequester(requester(KEY_PHONE)),
            )
            FormTextField(
                value = draft.personalInfo.location,
                onValueChange = {
                    draft = draft.copy(personalInfo = draft.personalInfo.copy(location = it))
                },
                label = stringResource(R.string.cv_editor_location),
                placeholder = stringResource(R.string.cv_example_location),
            )
            FormTextField(
                value = draft.personalInfo.bio.orEmpty(),
                onValueChange = {
                    draft = draft.copy(
                        personalInfo = draft.personalInfo.copy(bio = it.ifBlank { null }),
                    )
                    // Si lo corrige a mano, "Deshacer" ya no tiene sentido.
                    bioUndo = null
                },
                label = stringResource(R.string.cv_editor_bio),
                singleLine = false,
                imeAction = ImeAction.Default,
                placeholder = stringResource(R.string.cv_example_bio),
            )
            // La IA escribe el perfil con lo que ya tiene el CV (experiencias, estudios, habilidades).
            AiImproveRow(
                isLoading = aiState.isGeneratingBio,
                enabled = !aiState.isBusy,
                label = R.string.cv_ai_generate,
                hint = R.string.cv_ai_generate_bio_hint,
                showHint = draft.personalInfo.bio.isNullOrBlank(),
                errorMessage = aiState.bioError,
                canUndo = bioUndo != null,
                onImprove = { aiActions.onGenerateBio(draft) },
                onUndo = {
                    draft = draft.copy(personalInfo = draft.personalInfo.copy(bio = bioUndo?.ifBlank { null }))
                    bioUndo = null
                },
            )

            // ----- Experiencia laboral -----
            EditorSection(title = stringResource(R.string.cv_detail_experience))

            draft.workExperiences.forEachIndexed { index, experience ->
                EditorEntry(
                    // Mientras la IA trabaja no se puede quitar: correría la posición de la
                    // experiencia que está esperando su respuesta.
                    removeEnabled = !aiState.isBusy,
                    onRemove = {
                        draft = draft.copy(
                            workExperiences = draft.workExperiences.removeAt(index),
                        )
                        undo = null
                    },
                ) {
                    fun update(changed: WorkExperience) {
                        draft = draft.copy(
                            workExperiences = draft.workExperiences.replaceAt(index, changed),
                        )
                    }

                    FormTextField(
                        value = experience.position,
                        onValueChange = { update(experience.copy(position = it)) },
                        label = stringResource(R.string.cv_editor_position),
                        placeholder = stringResource(R.string.cv_example_position),
                        errorMessage = requiredMessage(showErrors && experience.missingPosition()),
                        modifier = Modifier.bringIntoViewRequester(requester("$KEY_EXPERIENCE_POSITION$index")),
                    )
                    FormTextField(
                        value = experience.company,
                        onValueChange = { update(experience.copy(company = it)) },
                        label = stringResource(R.string.cv_editor_company),
                        placeholder = stringResource(R.string.cv_example_company),
                        errorMessage = requiredMessage(showErrors && experience.missingCompany()),
                        // Fuera de un empleo formal, la empresa se puede dejar vacía.
                        helperText = if (experience.isFormal()) null else stringResource(R.string.cv_editor_company_optional),
                        modifier = Modifier.bringIntoViewRequester(requester("$KEY_EXPERIENCE_COMPANY$index")),
                    )
                    PeriodFields(
                        startDate = experience.startDate,
                        endDate = experience.endDate,
                        current = experience.current,
                        showErrors = showErrors,
                        onStartChange = { update(experience.copy(startDate = it)) },
                        onEndChange = { update(experience.copy(endDate = it)) },
                        // Marcar "Sigue vigente" y borrar la fecha de fin van en el mismo cambio.
                        // Antes eran dos seguidos y el segundo partía de la copia vieja, así que
                        // deshacía el primero y la casilla nunca quedaba marcada.
                        onCurrentChange = {
                            update(experience.copy(current = it, endDate = if (it) null else experience.endDate))
                        },
                        startModifier = Modifier.bringIntoViewRequester(requester("$KEY_EXPERIENCE$index")),
                    )
                    FormTextField(
                        value = experience.description,
                        onValueChange = {
                            update(experience.copy(description = it))
                            // Si la corrige a mano, "Deshacer" ya no tiene sentido.
                            if (undo?.first == index) undo = null
                        },
                        label = stringResource(R.string.cv_editor_description),
                        singleLine = false,
                        imeAction = ImeAction.Default,
                        placeholder = stringResource(R.string.cv_example_description),
                        errorMessage = requiredMessage(showErrors && experience.missingDescription()),
                        modifier = Modifier.bringIntoViewRequester(requester("$KEY_EXPERIENCE_DESCRIPTION$index")),
                    )
                    // Sin descripción, la IA la escribe desde cero con el puesto ("Generar con IA");
                    // con descripción, la redacta mejor ("Mejorar con IA").
                    val blankDescription = experience.description.isBlank()
                    AiImproveRow(
                        isLoading = aiState.expandingIndex == index,
                        enabled = !aiState.isBusy &&
                            (!blankDescription || experience.position.isNotBlank()),
                        label = if (blankDescription) R.string.cv_ai_generate else R.string.cv_ai_improve,
                        hint = R.string.cv_ai_generate_description_hint,
                        showHint = blankDescription,
                        errorMessage = aiState.expandError?.takeIf { it.index == index }?.message,
                        canUndo = undo?.first == index,
                        onImprove = {
                            if (blankDescription) {
                                aiActions.onGenerateDescription(index, experience)
                            } else {
                                aiActions.onExpand(index, experience)
                            }
                        },
                        onUndo = {
                            undo?.let { (_, original) -> update(experience.copy(description = original)) }
                            undo = null
                        },
                    )
                }
            }

            AddButton(
                text = stringResource(R.string.cv_editor_add_experience),
                onClick = {
                    draft = draft.copy(workExperiences = draft.workExperiences + emptyExperience())
                },
            )
            // Para quien no tiene empleos formales: voluntariados, proyectos del colegio o la
            // U, trabajos informales. La IA los pasa a lenguaje de CV.
            AddButton(
                text = stringResource(R.string.cv_ai_add_non_labor),
                onClick = { showNonLabor = true },
            )

            // ----- Educación -----
            EditorSection(title = stringResource(R.string.cv_detail_education))

            draft.education.forEachIndexed { index, study ->
                EditorEntry(
                    onRemove = { draft = draft.copy(education = draft.education.removeAt(index)) },
                ) {
                    fun update(changed: Education) {
                        draft = draft.copy(education = draft.education.replaceAt(index, changed))
                    }

                    FormTextField(
                        value = study.degree,
                        onValueChange = { update(study.copy(degree = it)) },
                        label = stringResource(R.string.cv_editor_degree),
                        placeholder = stringResource(R.string.cv_example_degree),
                    )
                    FormTextField(
                        value = study.institution,
                        onValueChange = { update(study.copy(institution = it)) },
                        label = stringResource(R.string.cv_editor_institution),
                        placeholder = stringResource(R.string.cv_example_institution),
                    )
                    FormTextField(
                        value = study.field,
                        onValueChange = { update(study.copy(field = it)) },
                        label = stringResource(R.string.cv_editor_field),
                        placeholder = stringResource(R.string.cv_example_field),
                    )
                    PeriodFields(
                        startDate = study.startDate,
                        endDate = study.endDate,
                        current = study.current,
                        showErrors = showErrors,
                        onStartChange = { update(study.copy(startDate = it)) },
                        onEndChange = { update(study.copy(endDate = it)) },
                        onCurrentChange = {
                            update(study.copy(current = it, endDate = if (it) null else study.endDate))
                        },
                        startModifier = Modifier.bringIntoViewRequester(requester("$KEY_EDUCATION$index")),
                    )
                }
            }

            AddButton(
                text = stringResource(R.string.cv_editor_add_education),
                onClick = { draft = draft.copy(education = draft.education + emptyEducation()) },
            )

            // ----- Habilidades -----
            EditorSection(title = stringResource(R.string.cv_detail_skills))

            draft.skills.forEachIndexed { index, skill ->
                EditorEntry(
                    onRemove = { draft = draft.copy(skills = draft.skills.removeAt(index)) },
                ) {
                    fun update(changed: Skill) {
                        draft = draft.copy(skills = draft.skills.replaceAt(index, changed))
                    }

                    FormTextField(
                        value = skill.name,
                        onValueChange = { update(skill.copy(name = it)) },
                        label = stringResource(R.string.cv_editor_skill_name),
                        placeholder = stringResource(R.string.cv_example_skill),
                        errorMessage = FormValidation.required(skill.name).messageOrNull(showErrors),
                    )
                    CvSkillLevelField(
                        value = skill.level,
                        onValueChange = { update(skill.copy(level = it)) },
                        errorMessage = FormValidation.skillLevel(skill.level)
                            .messageOrNull(showErrors || skill.level.isNotEmpty()),
                    )
                    FormTextField(
                        value = skill.category,
                        onValueChange = { update(skill.copy(category = it)) },
                        label = stringResource(R.string.cv_editor_category),
                        placeholder = stringResource(R.string.cv_example_category),
                    )
                }
            }

            AddButton(
                text = stringResource(R.string.cv_editor_add_skill),
                onClick = { draft = draft.copy(skills = draft.skills + emptySkill()) },
            )

            // ----- Idiomas -----
            EditorSection(title = stringResource(R.string.cv_detail_languages))

            draft.languages.forEachIndexed { index, language ->
                EditorEntry(
                    onRemove = { draft = draft.copy(languages = draft.languages.removeAt(index)) },
                ) {
                    fun update(changed: Language) {
                        draft = draft.copy(languages = draft.languages.replaceAt(index, changed))
                    }

                    FormTextField(
                        value = language.name,
                        onValueChange = { update(language.copy(name = it)) },
                        label = stringResource(R.string.cv_editor_language),
                        placeholder = stringResource(R.string.cv_example_language),
                    )
                    LevelField(
                        value = language.level,
                        options = LanguageLevel.entries,
                        onValueChange = { update(language.copy(level = it)) },
                    )
                }
            }

            AddButton(
                text = stringResource(R.string.cv_editor_add_language),
                onClick = { draft = draft.copy(languages = draft.languages + emptyLanguage()) },
            )

            // ----- Acciones -----
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
            ) {
                OutlinedButton(
                    onClick = ::requestClose,
                    enabled = !isSaving,
                    colors = outlinedOnCardColors(),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.cv_editor_cancel))
                }
                Button(
                    onClick = {
                        showErrors = true
                        if (!hasErrors) {
                            onSave(draft)
                        } else {
                            // Lleva al primer campo con error en vez de dejar al usuario buscándolo.
                            firstErrorKey()?.let { key -> scope.launch { requester(key).bringIntoView() } }
                        }
                    },
                    enabled = !isSaving,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        stringResource(
                            if (isSaving) R.string.cv_editor_saving else R.string.cv_editor_save,
                        ),
                    )
                }
            }
        }
    }
}

/** Título de sección con la línea que la separa de la anterior. */
@Composable
private fun EditorSection(
    title: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(top = 20.dp)) {
        HorizontalDivider()
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
        )
    }
}

/**
 * Una entrada de una lista, dentro de su propio recuadro.
 *
 * El recuadro es lo que deja ver dónde empieza y termina cada experiencia o estudio cuando
 * hay varios seguidos, que si no se mezclan en una sola columna de campos.
 */
@Composable
private fun EditorEntry(
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    removeEnabled: Boolean = true,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            content()
            TextButton(
                onClick = onRemove,
                enabled = removeEnabled,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(
                    text = stringResource(R.string.cv_editor_remove),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (removeEnabled) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_ALPHA)
                    },
                )
            }
        }
    }
}

/** Fechas de inicio y fin, con la casilla que marca que el periodo sigue vigente. */
@Composable
private fun PeriodFields(
    startDate: Long?,
    endDate: Long?,
    current: Boolean,
    showErrors: Boolean,
    onStartChange: (Long?) -> Unit,
    onEndChange: (Long?) -> Unit,
    onCurrentChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    startModifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        // "Desde" es obligatorio (ver missingStartDate en el editor).
        DateField(
            value = startDate,
            onValueChange = onStartChange,
            label = stringResource(R.string.cv_editor_start),
            modifier = startModifier,
            errorMessage = if (showErrors && startDate == null) {
                stringResource(R.string.validation_required)
            } else {
                null
            },
        )

        // Si está marcado "Actual" no tiene sentido pedir fecha de fin: el campo se apaga y
        // se manda en null, que es lo que espera el contrato.
        DateField(
            value = endDate,
            onValueChange = onEndChange,
            label = stringResource(R.string.cv_editor_end),
            enabled = !current,
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = current,
                // Quien llama borra la fecha de fin en el mismo cambio (ver onCurrentChange).
                onCheckedChange = onCurrentChange,
            )
            Text(
                text = stringResource(R.string.cv_editor_current),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/**
 * Campo de fecha que abre el calendario de Material 3 al tocarlo.
 *
 * Antes era texto libre ("AAAA-MM-DD"), pero el servicio de CVs ahora guarda las fechas en
 * epoch milisegundos: un texto mal escrito terminaba en un 400 al guardar. Con el calendario
 * el usuario no puede escribir una fecha inválida. El calendario devuelve la medianoche UTC del
 * día elegido, que es justo lo que espera el contrato (ver CvDates.kt).
 *
 * El campo de texto solo muestra la fecha; encima lleva una capa transparente que recibe el
 * toque, así nunca abre el teclado.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(
    value: Long?,
    onValueChange: (Long?) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    errorMessage: String? = null,
) {
    var picking by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxWidth()) {
        FormTextField(
            value = value.toCvDay(),
            onValueChange = {},
            label = label,
            enabled = enabled,
            errorMessage = errorMessage,
        )
        if (enabled) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(onClickLabel = stringResource(R.string.cv_editor_pick_date)) { picking = true },
            )
        }
    }

    if (picking) {
        val state = rememberDatePickerState(initialSelectedDateMillis = value)
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        onValueChange(state.selectedDateMillis)
                        picking = false
                    },
                ) {
                    Text(text = stringResource(R.string.cv_editor_date_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { picking = false }) {
                    Text(text = stringResource(R.string.cv_editor_cancel))
                }
            },
        ) {
            DatePicker(state = state)
        }
    }
}

/**
 * Nivel de una habilidad o un idioma. El servidor solo acepta los valores de su enum, así que
 * en vez de texto libre se elige de una lista: el usuario ve "Avanzado" y se guarda "advanced".
 * Si el CV trae un valor viejo que no está en la lista, se muestra tal cual hasta que lo cambie.
 */
@Composable
private fun LevelField(
    value: String,
    options: List<CvLevel>,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = options.firstOrNull { it.apiValue == value }

    Box(modifier = modifier.fillMaxWidth()) {
        FormTextField(
            value = selected?.let { stringResource(it.labelRes) } ?: value,
            onValueChange = {},
            label = stringResource(R.string.cv_editor_level),
        )
        // Capa encima del campo: tocarlo abre la lista en vez del teclado (igual que DateField).
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(onClickLabel = stringResource(R.string.cv_editor_pick_level)) { expanded = true },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(text = stringResource(option.labelRes)) },
                    onClick = {
                        onValueChange(option.apiValue)
                        expanded = false
                    },
                )
            }
        }
    }
}

/**
 * "Mejorar con IA" debajo de la descripción de una experiencia, con su estado: cargando,
 * error (el mismo botón sirve para reintentar) y "Deshacer" cuando la IA ya la redactó.
 */
@Composable
private fun AiImproveRow(
    isLoading: Boolean,
    enabled: Boolean,
    showHint: Boolean,
    @StringRes errorMessage: Int?,
    canUndo: Boolean,
    onImprove: () -> Unit,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier,
    @StringRes label: Int = R.string.cv_ai_improve,
    @StringRes hint: Int = R.string.cv_ai_improve_hint,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = onImprove,
                enabled = enabled,
                colors = outlinedOnCardColors(),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(16.dp),
                    )
                }
                Text(
                    text = stringResource(if (isLoading) R.string.cv_ai_improving else label),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            if (canUndo) {
                TextButton(onClick = onUndo) {
                    Text(text = stringResource(R.string.cv_ai_undo), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        val note = when {
            errorMessage != null -> errorMessage
            canUndo -> R.string.cv_ai_improved
            showHint -> hint
            else -> null
        }
        note?.let {
            Text(
                text = stringResource(it),
                style = MaterialTheme.typography.bodySmall,
                color = if (errorMessage != null) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

/**
 * Traductor de experiencia no laboral: el usuario cuenta con sus palabras qué hizo y la IA
 * lo devuelve como una experiencia de CV, que se agrega al final de la lista.
 *
 * Si se cierra mientras la IA trabaja, la respuesta se descarta.
 */
@Composable
private fun NonLaborDialog(
    isTranslating: Boolean,
    @StringRes errorMessage: Int?,
    onTranslate: (NonLaborExperience) -> Unit,
    onDismiss: () -> Unit,
) {
    var type by remember { mutableStateOf(NonLaborType.VOLUNTEER) }
    var organization by remember { mutableStateOf("") }
    var position by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val canTranslate = !isTranslating && position.isNotBlank() && description.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.cv_ai_non_labor_title)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = stringResource(R.string.cv_ai_non_labor_intro),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = stringResource(R.string.cv_ai_non_labor_type),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 12.dp),
                )
                NonLaborType.entries.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = type == option,
                                enabled = !isTranslating,
                                role = Role.RadioButton,
                                onClick = { type = option },
                            ),
                    ) {
                        // El clic lo maneja la fila entera, que es más fácil de tocar.
                        RadioButton(selected = type == option, onClick = null, enabled = !isTranslating)
                        Text(
                            text = stringResource(option.labelRes()),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 8.dp),
                        )
                    }
                }
                FormTextField(
                    value = organization,
                    onValueChange = { organization = it },
                    label = stringResource(R.string.cv_ai_non_labor_organization),
                    enabled = !isTranslating,
                )
                FormTextField(
                    value = position,
                    onValueChange = { position = it },
                    label = stringResource(R.string.cv_ai_non_labor_position),
                    enabled = !isTranslating,
                )
                FormTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = stringResource(R.string.cv_ai_non_labor_description),
                    singleLine = false,
                    imeAction = ImeAction.Default,
                    enabled = !isTranslating,
                )
                errorMessage?.let {
                    Text(
                        text = stringResource(it),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            // Si falló, el mismo botón sirve para reintentar.
            TextButton(
                onClick = { onTranslate(NonLaborExperience(type, organization, position, description)) },
                enabled = canTranslate,
            ) {
                if (isTranslating) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(16.dp),
                    )
                }
                Text(text = stringResource(if (isTranslating) R.string.cv_ai_translating else R.string.cv_ai_translate))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cv_editor_cancel))
            }
        },
    )
}

/** Nombre de cada tipo, el mismo que usa el PDF. */
@StringRes
private fun NonLaborType.labelRes(): Int = when (this) {
    NonLaborType.VOLUNTEER -> R.string.cv_experience_type_volunteer
    NonLaborType.SCHOOL_PROJECT -> R.string.cv_experience_type_school_project
    NonLaborType.INFORMAL -> R.string.cv_experience_type_informal
}

/** Opacidad de Material 3 para un texto deshabilitado. */
private const val DISABLED_ALPHA = 0.38f

/** Botón de agregar una entrada más a una lista. */
@Composable
private fun AddButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        colors = outlinedOnCardColors(),
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelMedium)
    }
}

/** Reemplaza el elemento de una posición sin tocar los demás. */
private fun <T> List<T>.replaceAt(index: Int, value: T): List<T> =
    toMutableList().also { it[index] = value }

/** Quita el elemento de una posición. */
private fun <T> List<T>.removeAt(index: Int): List<T> =
    toMutableList().also { it.removeAt(index) }

// Entradas nuevas: van sin id porque el id lo pone el servicio al guardarlas.
private fun emptyExperience() = WorkExperience(
    id = "",
    company = "",
    position = "",
    startDate = null,
    endDate = null,
    current = false,
    description = "",
    // El contrato exige un tipo y no acepta texto vacío. El editor todavía no deja elegirlo,
    // así que una experiencia nueva arranca como empleo formal.
    type = "formal",
)

private fun emptyEducation() = Education(
    id = "",
    institution = "",
    degree = "",
    field = "",
    startDate = null,
    endDate = null,
    current = false,
)

// El nivel es obligatorio en el contrato: se arranca con uno válido para no mandar "" y recibir 400.
private fun emptySkill() = Skill(id = "", name = "", level = SkillLevel.INTERMEDIATE.apiValue, category = "")

private fun emptyLanguage() = Language(id = "", name = "", level = LanguageLevel.INTERMEDIATE.apiValue)

/** El mensaje de error solo se muestra una vez que el usuario intentó guardar. */
@Composable
private fun Int?.messageOrNull(show: Boolean): String? =
    if (show && this != null) stringResource(this) else null

/** Claves de los campos que pueden fallar, para desplazarse al primero con error. */
private const val KEY_TITLE = "title"
private const val KEY_NAME = "name"
private const val KEY_EMAIL = "email"
private const val KEY_PHONE = "phone"
private const val KEY_EXPERIENCE = "experience-"
private const val KEY_EDUCATION = "education-"
private const val KEY_EXPERIENCE_POSITION = "experience-position-"
private const val KEY_EXPERIENCE_COMPANY = "experience-company-"
private const val KEY_EXPERIENCE_DESCRIPTION = "experience-description-"

/** Empleo con una empresa. Las experiencias viejas sin tipo cuentan como formales. */
private fun WorkExperience.isFormal(): Boolean = type.isBlank() || type == "formal"

private fun WorkExperience.missingPosition(): Boolean = position.isBlank()

private fun WorkExperience.missingDescription(): Boolean = description.isBlank()

/** La empresa solo falta si es un empleo formal; en lo demás es opcional. */
private fun WorkExperience.missingCompany(): Boolean = isFormal() && company.isBlank()

/** "Este campo es obligatorio." cuando [show] es verdadero; si no, null (sin error). */
@Composable
private fun requiredMessage(show: Boolean): String? =
    if (show) stringResource(R.string.validation_required) else null
