package com.cvpro.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.cvpro.app.R
import com.cvpro.app.domain.model.Cv
import com.cvpro.app.domain.model.Education
import com.cvpro.app.domain.model.Language
import com.cvpro.app.domain.model.Skill
import com.cvpro.app.domain.model.WorkExperience

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
) {
    // El borrador arranca del currículum recibido y se reinicia si llega otro distinto.
    var draft by remember(cv.id) { mutableStateOf(cv) }
    // Los errores solo aparecen después del primer intento de guardar, para no pintar el
    // formulario en rojo apenas se abre.
    var showErrors by remember(cv.id) { mutableStateOf(false) }

    val titleError = FormValidation.required(draft.title)
    val emailError = FormValidation.email(draft.personalInfo.email)
    val nameError = FormValidation.required(draft.personalInfo.name)
    val phoneError = FormValidation.phoneOptional(draft.personalInfo.phone.orEmpty())
    val hasErrors = listOf(titleError, emailError, nameError, phoneError).any { it != null }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier,
    ) {
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
                modifier = Modifier.padding(top = 12.dp),
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
            )
            FormTextField(
                value = draft.personalInfo.email,
                onValueChange = {
                    draft = draft.copy(personalInfo = draft.personalInfo.copy(email = it))
                },
                label = stringResource(R.string.cv_editor_email),
                errorMessage = emailError.messageOrNull(showErrors),
                keyboardType = KeyboardType.Email,
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
            )
            FormTextField(
                value = draft.personalInfo.location,
                onValueChange = {
                    draft = draft.copy(personalInfo = draft.personalInfo.copy(location = it))
                },
                label = stringResource(R.string.cv_editor_location),
            )
            FormTextField(
                value = draft.personalInfo.bio.orEmpty(),
                onValueChange = {
                    draft = draft.copy(
                        personalInfo = draft.personalInfo.copy(bio = it.ifBlank { null }),
                    )
                },
                label = stringResource(R.string.cv_editor_bio),
                singleLine = false,
                imeAction = ImeAction.Default,
            )

            // ----- Experiencia laboral -----
            EditorSection(title = stringResource(R.string.cv_detail_experience))

            draft.workExperiences.forEachIndexed { index, experience ->
                EditorEntry(
                    onRemove = {
                        draft = draft.copy(
                            workExperiences = draft.workExperiences.removeAt(index),
                        )
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
                    )
                    FormTextField(
                        value = experience.company,
                        onValueChange = { update(experience.copy(company = it)) },
                        label = stringResource(R.string.cv_editor_company),
                    )
                    PeriodFields(
                        startDate = experience.startDate,
                        endDate = experience.endDate,
                        current = experience.current,
                        onStartChange = { update(experience.copy(startDate = it)) },
                        onEndChange = { update(experience.copy(endDate = it)) },
                        onCurrentChange = { update(experience.copy(current = it)) },
                    )
                    FormTextField(
                        value = experience.description,
                        onValueChange = { update(experience.copy(description = it)) },
                        label = stringResource(R.string.cv_editor_description),
                        singleLine = false,
                        imeAction = ImeAction.Default,
                    )
                }
            }

            AddButton(
                text = stringResource(R.string.cv_editor_add_experience),
                onClick = {
                    draft = draft.copy(workExperiences = draft.workExperiences + emptyExperience())
                },
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
                    )
                    FormTextField(
                        value = study.institution,
                        onValueChange = { update(study.copy(institution = it)) },
                        label = stringResource(R.string.cv_editor_institution),
                    )
                    FormTextField(
                        value = study.field,
                        onValueChange = { update(study.copy(field = it)) },
                        label = stringResource(R.string.cv_editor_field),
                    )
                    PeriodFields(
                        startDate = study.startDate,
                        endDate = study.endDate,
                        current = study.current,
                        onStartChange = { update(study.copy(startDate = it)) },
                        onEndChange = { update(study.copy(endDate = it)) },
                        onCurrentChange = { update(study.copy(current = it)) },
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
                    )
                    FormTextField(
                        value = skill.level,
                        onValueChange = { update(skill.copy(level = it)) },
                        label = stringResource(R.string.cv_editor_level),
                    )
                    FormTextField(
                        value = skill.category,
                        onValueChange = { update(skill.copy(category = it)) },
                        label = stringResource(R.string.cv_editor_category),
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
                    )
                    FormTextField(
                        value = language.level,
                        onValueChange = { update(language.copy(level = it)) },
                        label = stringResource(R.string.cv_editor_level),
                        imeAction = ImeAction.Done,
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
                    onClick = onDismiss,
                    enabled = !isSaving,
                    colors = outlinedOnCardColors(),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.cv_editor_cancel))
                }
                Button(
                    onClick = {
                        showErrors = true
                        if (!hasErrors) onSave(draft)
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
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(
                    text = stringResource(R.string.cv_editor_remove),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

/** Fechas de inicio y fin, con la casilla que marca que el periodo sigue vigente. */
@Composable
private fun PeriodFields(
    startDate: String,
    endDate: String?,
    current: Boolean,
    onStartChange: (String) -> Unit,
    onEndChange: (String?) -> Unit,
    onCurrentChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        FormTextField(
            value = startDate,
            onValueChange = onStartChange,
            label = stringResource(R.string.cv_editor_start),
        )

        // Si está marcado "Actual" no tiene sentido pedir fecha de fin: el campo se apaga y
        // se manda en null, que es lo que espera el contrato.
        FormTextField(
            value = endDate.orEmpty(),
            onValueChange = { onEndChange(it.ifBlank { null }) },
            label = stringResource(R.string.cv_editor_end),
            enabled = !current,
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = current,
                onCheckedChange = { marcado ->
                    onCurrentChange(marcado)
                    if (marcado) onEndChange(null)
                },
            )
            Text(
                text = stringResource(R.string.cv_editor_current),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

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
    startDate = "",
    endDate = null,
    current = false,
    description = "",
    type = "",
)

private fun emptyEducation() = Education(
    id = "",
    institution = "",
    degree = "",
    field = "",
    startDate = "",
    endDate = null,
    current = false,
)

private fun emptySkill() = Skill(id = "", name = "", level = "", category = "")

private fun emptyLanguage() = Language(id = "", name = "", level = "")

/** El mensaje de error solo se muestra una vez que el usuario intentó guardar. */
@Composable
private fun Int?.messageOrNull(show: Boolean): String? =
    if (show && this != null) stringResource(this) else null
