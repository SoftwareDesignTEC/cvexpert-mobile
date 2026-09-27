package com.cvpro.app.ui.screens.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
    } else {
        ProfileForm(
            uiState = uiState,
            onNameChange = viewModel::onNameChange,
            onEmailChange = viewModel::onEmailChange,
            onPhoneChange = viewModel::onPhoneChange,
            onLocationChange = viewModel::onLocationChange,
            onSaveClick = viewModel::onSaveClick,
            modifier = modifier,
        )
    }
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
