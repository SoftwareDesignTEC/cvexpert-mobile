package com.cvpro.app.ui.screens.cvs

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cvpro.app.R
import com.cvpro.app.ui.components.FormTextField

/**
 * Modo IA: el usuario cuenta su experiencia con sus palabras (escrita o dictada con el
 * micrófono del teclado) y el servicio de CVs arma el currículum completo.
 *
 * Cuando el CV se crea, se llama a [onCreated]; quien abre la hoja lleva al usuario a Mis
 * Currículums, donde el CV se abre solo en el editor para revisarlo.
 */
@Composable
fun AiCvSheet(
    onDismiss: () -> Unit,
    onCreated: () -> Unit,
    viewModel: AiCvViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.onOpened() }
    LaunchedEffect(uiState.created) {
        if (uiState.created) {
            viewModel.onCreatedHandled()
            onCreated()
        }
    }

    AiCvContent(
        uiState = uiState,
        onDismiss = onDismiss,
        onFullNameChange = viewModel::onFullNameChange,
        onPhoneChange = viewModel::onPhoneChange,
        onFreeTextChange = viewModel::onFreeTextChange,
        onTemplateChange = viewModel::onTemplateChange,
        onGenerate = viewModel::generate,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AiCvContent(
    uiState: AiCvUiState,
    onDismiss: () -> Unit,
    onFullNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onFreeTextChange: (String) -> Unit,
    onTemplateChange: (String) -> Unit,
    onGenerate: () -> Unit,
) {
    ModalBottomSheet(
        // Mientras la IA trabaja no se cierra por accidente: se perdería la espera.
        onDismissRequest = { if (!uiState.isGenerating) onDismiss() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                text = stringResource(R.string.ai_cv_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.ai_cv_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )

            FormTextField(
                value = uiState.fullName,
                onValueChange = onFullNameChange,
                label = stringResource(R.string.cv_editor_name),
                errorMessage = if (uiState.showErrors && uiState.nameMissing) {
                    stringResource(R.string.validation_required)
                } else {
                    null
                },
                enabled = !uiState.isGenerating,
            )
            FormTextField(
                value = uiState.phone,
                onValueChange = onPhoneChange,
                label = stringResource(R.string.cv_editor_phone),
                keyboardType = KeyboardType.Phone,
                placeholder = stringResource(R.string.cv_example_phone),
                enabled = !uiState.isGenerating,
            )

            FormTextField(
                value = uiState.freeText,
                onValueChange = onFreeTextChange,
                label = stringResource(R.string.ai_cv_free_text),
                placeholder = stringResource(R.string.ai_cv_free_text_example),
                helperText = stringResource(R.string.ai_cv_free_text_hint),
                errorMessage = if (uiState.showErrors && uiState.textTooShort) {
                    stringResource(R.string.ai_cv_free_text_short)
                } else {
                    null
                },
                singleLine = false,
                imeAction = ImeAction.Default,
                enabled = !uiState.isGenerating,
                modifier = Modifier.heightIn(min = 160.dp),
            )

            if (uiState.templates.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.ai_cv_template),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(top = 4.dp),
                ) {
                    uiState.templates.forEach { template ->
                        FilterChip(
                            selected = template.id == uiState.templateId,
                            onClick = { onTemplateChange(template.id) },
                            enabled = !uiState.isGenerating,
                            label = { Text(template.name) },
                        )
                    }
                }
            }

            uiState.error?.let {
                Text(
                    text = stringResource(it),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }

            Button(
                onClick = onGenerate,
                enabled = !uiState.isGenerating,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
            ) {
                if (uiState.isGenerating) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(16.dp),
                    )
                }
                Text(
                    stringResource(if (uiState.isGenerating) R.string.ai_cv_generating else R.string.ai_cv_generate),
                )
            }
            if (uiState.isGenerating) {
                Text(
                    text = stringResource(R.string.ai_cv_wait),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}
