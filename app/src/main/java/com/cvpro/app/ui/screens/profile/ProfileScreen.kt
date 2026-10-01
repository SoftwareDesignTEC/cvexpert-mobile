package com.cvpro.app.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.height
import com.cvpro.app.ui.components.RadarChart
import com.cvpro.app.ui.components.RadarSeries
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.cvpro.app.domain.model.JobModality
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cvpro.app.R
import com.cvpro.app.ui.components.outlinedOnCardColors
import com.cvpro.app.ui.components.FormTextField
import com.cvpro.app.ui.components.LocalSnackbarHostState
import com.cvpro.app.ui.components.PrimaryButton
import com.cvpro.app.ui.theme.CvProTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment

/**
 * Mi Perfil con estado: conecta el ViewModel y muestra los avisos en el snackbar de la app.
 */
@Composable
fun ProfileScreen(
    onSkillsGapClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = LocalSnackbarHostState.current

    // Avisos de una sola vez. Se guardan con un número que sube en cada emisión, para que
    // el mismo mensaje dos veces seguidas también se muestre dos veces.
    var pendingMessage by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    LaunchedEffect(Unit) {
        var seq = 0
        viewModel.messages.collect { messageRes ->
            seq++
            pendingMessage = seq to messageRes
        }
    }
    // El texto se resuelve acá y no dentro del efecto: así sigue el idioma del teléfono.
    val messageText = pendingMessage?.let { (_, messageRes) -> stringResource(messageRes) }
    LaunchedEffect(pendingMessage) {
        if (messageText != null) snackbarHostState.showSnackbar(messageText)
    }

    if (uiState.isLoading) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
    } else if (uiState.isEditing) {
        ProfileForm(
            uiState = uiState,
            onNameChange = viewModel::onNameChange,
            onEmailChange = viewModel::onEmailChange,
            onPhoneChange = viewModel::onPhoneChange,
            onLocationChange = viewModel::onLocationChange,
            onSaveClick = viewModel::onSaveClick,
            onCancelClick = viewModel::onCancelEdit,
            modifier = modifier,
        )
    } else {
        ProfileCard(
            uiState = uiState,
            onEditClick = viewModel::onEditClick,
            onSkillsGapClick = onSkillsGapClick,
            modifier = modifier,
        )
    }
}

/**
 * Ficha del perfil, como en el diseño de Samuel.
 *
 * Es de solo lectura: el encabezado con la foto y las etiquetas, la descripción, y abajo los
 * bloques de contacto y preferencias. Editar abre el formulario.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileCard(
    uiState: ProfileUiState,
    onEditClick: () -> Unit,
    onSkillsGapClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val profile = uiState.profile

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column {
            Text(
                text = stringResource(R.string.profile_eyebrow),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.profile_section_personal),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        // Ficha principal: foto, nombre, etiquetas y descripción.
        ProfileSurface {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProfileAvatar(photoUrl = profile?.photoUrl, name = uiState.name)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = uiState.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            // Etiquetas: residencia, disponibilidad, modalidad y sectores de interés.
            val chips = buildList {
                if (uiState.location.isNotBlank()) add(uiState.location)
                profile?.types?.forEach { add(it) }
                profile?.modalities?.forEach { add(stringResource(it.labelRes())) }
                profile?.sectors?.forEach { add(it) }
            }
            if (chips.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 12.dp),
                ) {
                    chips.forEach { chip -> ProfileChip(text = chip) }
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            ) {
                Text(
                    text = profile?.bio?.takeIf { it.isNotBlank() }
                        ?: stringResource(R.string.profile_no_bio),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp),
                )
            }

            PrimaryButton(
                text = stringResource(R.string.profile_edit),
                onClick = onEditClick,
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        // Análisis de habilidades: el mismo radar del Panel Principal.
        val analysis = uiState.skillsGap
        if (analysis != null) {
            ProfileSurface {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(width = 4.dp, height = 18.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.primary),
                    )
                    Text(
                        text = stringResource(R.string.home_skills_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }

                RadarChart(
                    labels = analysis.skills.map { it.name },
                    series = listOf(
                        RadarSeries(
                            label = stringResource(R.string.skills_gap_series_user),
                            values = analysis.skills.map { it.userLevel },
                            color = MaterialTheme.colorScheme.primary,
                        ),
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .padding(top = 12.dp),
                )

                OutlinedButton(
                    onClick = onSkillsGapClick,
                    colors = outlinedOnCardColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                ) {
                    Text(stringResource(R.string.home_shortcut_skills_gap))
                }
            }
        }

        // Datos de contacto.
        ProfileSurface {
            Text(
                text = stringResource(R.string.profile_contact_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            ProfileField(
                label = stringResource(R.string.profile_contact_phone),
                value = uiState.phone,
            )
            ProfileField(
                label = stringResource(R.string.profile_contact_email),
                value = uiState.email,
            )
            ProfileField(
                label = stringResource(R.string.profile_contact_residence),
                value = uiState.location,
            )
            OutlinedButton(
                onClick = onEditClick,
                colors = outlinedOnCardColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            ) {
                Text(stringResource(R.string.profile_edit_contact))
            }
        }

        // Preferencias laborales.
        ProfileSurface {
            Text(
                text = stringResource(R.string.profile_preferences_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            ProfileField(
                label = stringResource(R.string.profile_pref_sector),
                value = profile?.sectors?.joinToString(", ").orEmpty(),
            )
            ProfileField(
                label = stringResource(R.string.profile_pref_availability),
                value = profile?.types?.joinToString(", ").orEmpty(),
            )
            // Los nombres se resuelven con map, que es inline y sí admite stringResource;
            // joinToString no lo es, y ahí el compilador lo rechaza.
            val modalityLabels = profile?.modalities
                ?.map { stringResource(it.labelRes()) }
                .orEmpty()
            ProfileField(
                label = stringResource(R.string.profile_pref_modality),
                value = modalityLabels.joinToString(", "),
            )
            // TODO: sin acción. El endpoint de perfil solo acepta nombre, correo, teléfono y
            // residencia: todavía no se pueden guardar sectores, disponibilidad ni modalidad.
            OutlinedButton(
                onClick = {},
                colors = outlinedOnCardColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            ) {
                Text(stringResource(R.string.profile_edit_preferences))
            }
        }

        Spacer(modifier = Modifier.padding(bottom = 8.dp))
    }
}

/** Tarjeta blanca de una sección del perfil. */
@Composable
private fun ProfileSurface(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

/** Una etiqueta del perfil: residencia, disponibilidad, modalidad o sector. */
@Composable
private fun ProfileChip(
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(50),
        modifier = modifier,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

/** Un dato del perfil, con su etiqueta arriba. */
@Composable
private fun ProfileField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(top = 10.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value.takeIf { it.isNotBlank() } ?: stringResource(R.string.profile_not_set),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Foto de la cuenta, o la inicial del nombre si no hay. */
@Composable
private fun ProfileAvatar(
    photoUrl: String?,
    name: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(64.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (photoUrl != null) {
            AsyncImage(
                model = photoUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = name.take(1).uppercase(),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private fun JobModality.labelRes(): Int = when (this) {
    JobModality.REMOTE -> R.string.modality_remote
    JobModality.ON_SITE -> R.string.modality_on_site
    JobModality.HYBRID -> R.string.modality_hybrid
}

/**
 * Formulario sin estado: recibe los valores y avisa los cambios hacia arriba.
 *
 * Cada campo usa el componente FormTextField, así que todos validan y muestran los errores
 * igual. El teclado cambia según el campo: correo, teléfono o texto.
 */
@Composable
fun ProfileForm(
    uiState: ProfileUiState,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onLocationChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier,
    onCancelClick: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Text(
            text = stringResource(R.string.profile_section_personal),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = stringResource(R.string.profile_pending_notice),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
        )

        FormTextField(
            value = uiState.name,
            onValueChange = onNameChange,
            label = stringResource(R.string.profile_field_name),
            errorMessage = uiState.nameError?.let { stringResource(it) },
        )
        FormTextField(
            value = uiState.email,
            onValueChange = onEmailChange,
            label = stringResource(R.string.profile_field_email),
            errorMessage = uiState.emailError?.let { stringResource(it) },
            keyboardType = KeyboardType.Email,
            modifier = Modifier.padding(top = 12.dp),
        )
        FormTextField(
            value = uiState.phone,
            onValueChange = onPhoneChange,
            label = stringResource(R.string.profile_field_phone),
            errorMessage = uiState.phoneError?.let { stringResource(it) },
            helperText = stringResource(R.string.profile_field_phone_helper),
            keyboardType = KeyboardType.Phone,
            modifier = Modifier.padding(top = 12.dp),
        )
        FormTextField(
            value = uiState.location,
            onValueChange = onLocationChange,
            label = stringResource(R.string.profile_field_location),
            errorMessage = uiState.locationError?.let { stringResource(it) },
            imeAction = ImeAction.Done,
            modifier = Modifier.padding(top = 12.dp),
        )

        PrimaryButton(
            text = stringResource(R.string.profile_save),
            onClick = onSaveClick,
            enabled = uiState.canSave,
            modifier = Modifier.padding(top = 24.dp),
        )
        OutlinedButton(
            onClick = onCancelClick,
            colors = outlinedOnCardColors(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        ) {
            Text(stringResource(R.string.profile_edit_cancel))
        }
    }
}

@Preview(name = "Perfil - vacío")
@Composable
private fun ProfileFormPreview() {
    CvProTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            ProfileForm(
                uiState = ProfileUiState(name = "Andrés Zumbado", email = "andres@ejemplo.com"),
                onNameChange = {},
                onEmailChange = {},
                onPhoneChange = {},
                onLocationChange = {},
                onSaveClick = {},
            )
        }
    }
}

@Preview(name = "Perfil - con errores")
@Composable
private fun ProfileFormErrorPreview() {
    CvProTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            ProfileForm(
                uiState = ProfileUiState(
                    name = "An",
                    email = "correo-malo",
                    phone = "123",
                    location = "",
                    nameError = R.string.validation_min_length,
                    emailError = R.string.validation_email,
                    phoneError = R.string.validation_phone,
                    locationError = R.string.validation_required,
                ),
                onNameChange = {},
                onEmailChange = {},
                onPhoneChange = {},
                onLocationChange = {},
                onSaveClick = {},
            )
        }
    }
}
