package com.cvpro.app.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cvpro.app.R
import com.cvpro.app.domain.model.ThemePreference
import com.cvpro.app.ui.theme.CvProTheme

/** Configuración con estado. */
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SettingsContent(
        uiState = uiState,
        onThemeSelected = viewModel::onThemeSelected,
        modifier = modifier,
    )
}

/**
 * Configuración sin estado.
 *
 * El tema se elige entre tres opciones y queda guardado en el teléfono: al volver a abrir la
 * app se respeta lo elegido, que es lo que pide el enunciado.
 */
@Composable
fun SettingsContent(
    uiState: SettingsUiState,
    onThemeSelected: (ThemePreference) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_theme_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = stringResource(R.string.settings_theme_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
        )

        // selectableGroup avisa a los lectores de pantalla que las tres opciones son
        // excluyentes, como un grupo de radio buttons.
        Column(modifier = Modifier.selectableGroup()) {
            ThemePreference.entries.forEach { preference ->
                ThemeOptionRow(
                    label = stringResource(preference.labelRes()),
                    selected = preference == uiState.themePreference,
                    onSelect = { onThemeSelected(preference) },
                )
            }
        }
    }
}

/** Una opción de tema: toda la fila es tocable, no solo el círculo. */
@Composable
private fun ThemeOptionRow(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton)
            .padding(vertical = 12.dp),
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

private fun ThemePreference.labelRes(): Int = when (this) {
    ThemePreference.SYSTEM -> R.string.settings_theme_system
    ThemePreference.LIGHT -> R.string.settings_theme_light
    ThemePreference.DARK -> R.string.settings_theme_dark
}

@Preview(name = "Configuración - claro")
@Composable
private fun SettingsContentPreview() {
    CvProTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            SettingsContent(uiState = SettingsUiState(ThemePreference.LIGHT), onThemeSelected = {})
        }
    }
}

@Preview(name = "Configuración - oscuro")
@Composable
private fun SettingsContentDarkPreview() {
    CvProTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            SettingsContent(uiState = SettingsUiState(ThemePreference.DARK), onThemeSelected = {})
        }
    }
}
