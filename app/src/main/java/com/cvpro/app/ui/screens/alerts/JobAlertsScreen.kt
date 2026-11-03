package com.cvpro.app.ui.screens.alerts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cvpro.app.R
import com.cvpro.app.domain.model.JobModality
import com.cvpro.app.ui.components.ErrorState
import com.cvpro.app.ui.components.FormTextField
import com.cvpro.app.ui.components.LoadingState
import com.cvpro.app.ui.components.PrimaryButton

/**
 * Alertas de ofertas nuevas, a pantalla completa sobre Configuración.
 *
 * Es un diálogo y no una ruta del grafo porque la barra superior de la app toma su título del
 * menú lateral y no tiene flecha de volver; así esta pantalla trae su propia barra.
 *
 * Tiene su propio SnackbarHost: el de la app queda detrás del diálogo y no se vería.
 */
@Composable
fun JobAlertsDialog(
    onDismiss: () -> Unit,
    viewModel: JobAlertsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { messageRes ->
            snackbarHostState.showSnackbar(context.getString(messageRes))
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        // Ocupa toda la pantalla en vez de la tarjeta centrada de un diálogo normal.
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        JobAlertsContent(
            uiState = uiState,
            snackbarHostState = snackbarHostState,
            onBack = onDismiss,
            onRetry = viewModel::load,
            onEnabledChange = viewModel::onEnabledChange,
            onMinMatchChange = viewModel::onMinMatchChange,
            onSkillInputChange = viewModel::onSkillInputChange,
            onAddSkill = viewModel::onAddSkill,
            onRemoveSkill = viewModel::onRemoveSkill,
            onExperienceChange = viewModel::onExperienceChange,
            onSave = viewModel::onSaveClick,
        )
    }
}

/** Contenido sin estado: recibe el UiState y avisa cada acción con una lambda. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobAlertsContent(
    uiState: JobAlertsUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onMinMatchChange: (Float) -> Unit,
    onSkillInputChange: (String) -> Unit,
    onAddSkill: () -> Unit,
    onRemoveSkill: (String) -> Unit,
    onExperienceChange: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.alerts_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.alerts_back),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize(),
    ) { padding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(padding)
        when {
            uiState.isLoading -> LoadingState(modifier = contentModifier)
            uiState.loadFailed -> ErrorState(
                message = stringResource(R.string.alerts_load_error),
                onRetryClick = onRetry,
                modifier = contentModifier,
            )
            else -> AlertsForm(
                uiState = uiState,
                onEnabledChange = onEnabledChange,
                onMinMatchChange = onMinMatchChange,
                onSkillInputChange = onSkillInputChange,
                onAddSkill = onAddSkill,
                onRemoveSkill = onRemoveSkill,
                onExperienceChange = onExperienceChange,
                onSave = onSave,
                modifier = contentModifier,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AlertsForm(
    uiState: JobAlertsUiState,
    onEnabledChange: (Boolean) -> Unit,
    onMinMatchChange: (Float) -> Unit,
    onSkillInputChange: (String) -> Unit,
    onAddSkill: () -> Unit,
    onRemoveSkill: (String) -> Unit,
    onExperienceChange: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Interruptor y mínimo de compatibilidad.
        AlertsCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.alerts_enabled),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.alerts_enabled_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = uiState.enabled, onCheckedChange = onEnabledChange)
            }

            Text(
                text = stringResource(R.string.alerts_min_match, uiState.minMatch),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 16.dp),
            )
            Slider(
                value = uiState.minMatch.toFloat(),
                onValueChange = onMinMatchChange,
                valueRange = 0f..100f,
                // 19 pasos intermedios = de 5 en 5 entre 0 y 100.
                steps = 19,
                enabled = uiState.enabled,
            )
        }

        // Lo que el perfil de Usuarios no tiene: habilidades y experiencia.
        AlertsCard {
            SectionTitle(
                title = stringResource(R.string.alerts_skills_title),
                subtitle = stringResource(R.string.alerts_skills_hint),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp),
            ) {
                OutlinedTextField(
                    value = uiState.skillInput,
                    onValueChange = onSkillInputChange,
                    label = { Text(stringResource(R.string.alerts_skill_input)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onAddSkill() }),
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = onAddSkill,
                    enabled = uiState.skillInput.isNotBlank(),
                ) {
                    Text(stringResource(R.string.alerts_skill_add))
                }
            }
            if (uiState.skills.isEmpty()) {
                Text(
                    text = stringResource(R.string.alerts_skills_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    uiState.skills.forEach { skill ->
                        val removeLabel = stringResource(R.string.alerts_skill_remove, skill)
                        InputChip(
                            selected = false,
                            onClick = { onRemoveSkill(skill) },
                            label = { Text(skill) },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = removeLabel,
                                    modifier = Modifier.size(InputChipDefaults.IconSize),
                                )
                            },
                        )
                    }
                }
            }

            FormTextField(
                value = uiState.experienceYears,
                onValueChange = onExperienceChange,
                label = stringResource(R.string.alerts_experience),
                errorMessage = uiState.experienceError?.let { stringResource(it) },
                helperText = stringResource(R.string.alerts_experience_hint),
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        // Lo que se copia del perfil: se muestra para que se entienda con qué se compara.
        AlertsCard {
            SectionTitle(
                title = stringResource(R.string.alerts_profile_title),
                subtitle = stringResource(R.string.alerts_profile_hint),
            )
            val modalityLabels = uiState.modalities.map { stringResource(it.labelRes()) }
            val jobTypeLabels = uiState.jobTypes.map { stringResource(jobTypeLabelRes(it)) }
            ProfileLine(stringResource(R.string.alerts_profile_location), uiState.location)
            ProfileLine(stringResource(R.string.alerts_profile_modalities), modalityLabels.joinToString(", "))
            ProfileLine(stringResource(R.string.alerts_profile_job_types), jobTypeLabels.joinToString(", "))
            ProfileLine(stringResource(R.string.alerts_profile_sectors), uiState.sectors.joinToString(", "))
        }

        PrimaryButton(
            text = stringResource(R.string.alerts_save),
            onClick = onSave,
            isLoading = uiState.isSaving,
            modifier = Modifier.padding(vertical = 8.dp),
        )
    }
}

@Composable
private fun AlertsCard(content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) { content() }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Una línea "Etiqueta: valor"; sin valor muestra un guion para que se note que falta. */
@Composable
private fun ProfileLine(label: String, value: String) {
    Text(
        text = stringResource(R.string.alerts_profile_line, label, value.ifBlank { "—" }),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = 8.dp),
    )
}

private fun JobModality.labelRes(): Int = when (this) {
    JobModality.REMOTE -> R.string.modality_remote
    JobModality.ON_SITE -> R.string.modality_on_site
    JobModality.HYBRID -> R.string.modality_hybrid
}

/** Nombre legible de cada jornada del contrato. Si llega otra, se muestra "Otra". */
private fun jobTypeLabelRes(type: String): Int = when (type) {
    "full-time" -> R.string.job_type_full_time
    "part-time" -> R.string.job_type_part_time
    "freelance" -> R.string.job_type_freelance
    "internship" -> R.string.job_type_internship
    else -> R.string.job_type_other
}
