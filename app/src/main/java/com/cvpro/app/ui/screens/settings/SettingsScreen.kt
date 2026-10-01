package com.cvpro.app.ui.screens.settings

import android.content.Intent
import androidx.core.net.toUri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cvpro.app.R
import com.cvpro.app.domain.model.ThemePreference
import com.cvpro.app.domain.repository.NotificationSettings
import com.cvpro.app.domain.repository.NotificationType
import com.cvpro.app.ui.theme.CvProTheme

/** Configuración con estado. */
@Composable
fun SettingsScreen(
    onSignOutClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SettingsContent(
        uiState = uiState,
        onThemeSelected = viewModel::onThemeSelected,
        onNotificationChange = viewModel::onNotificationChange,
        onSignOutClick = onSignOutClick,
        modifier = modifier,
    )
}

/**
 * Configuración, siguiendo el diseño de Samuel.
 *
 * De los cuatro bloques, el único que hace algo hoy es el tema, que se guarda en el
 * dispositivo. El resto está dibujado y marcado con TODO: son opciones que necesitan
 * pantallas o endpoints que la Fase I no tiene.
 */
@Composable
fun SettingsContent(
    uiState: SettingsUiState,
    onThemeSelected: (ThemePreference) -> Unit,
    onNotificationChange: (NotificationType, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onSignOutClick: () -> Unit = {},
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column {
            Text(
                text = stringResource(R.string.settings_eyebrow),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.destination_settings),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        // Seguridad. Ninguna opción aplica todavía: la sesión es de Google, así que no hay
        // contraseña ni correo propios que cambiar.
        // TODO: revisar con Samuel si estas opciones tienen sentido con login de Google.
        SettingsCard(
            title = stringResource(R.string.settings_security_title),
            subtitle = stringResource(R.string.settings_security_subtitle),
        ) {
            SettingsRow(label = stringResource(R.string.settings_security_sessions))
            // El cierre de sesión vive acá y no en el menú lateral, que en el diseño de
            // Samuel no lo tiene. Es la única forma de salir de la cuenta.
            SettingsRow(
                label = stringResource(R.string.home_sign_out),
                onClick = onSignOutClick,
            )
        }

        // Los avisos son preferencias de encender y apagar, no accesos a otra pantalla, así
        // que llevan interruptor en vez de flecha.
        SettingsCard(
            title = stringResource(R.string.settings_notifications_title),
            subtitle = stringResource(R.string.settings_notifications_subtitle),
        ) {
            NotificationRow(
                label = stringResource(R.string.settings_notifications_applications),
                type = NotificationType.APPLICATION_STATUS,
                settings = uiState.notifications,
                onChange = onNotificationChange,
            )
            NotificationRow(
                label = stringResource(R.string.settings_notifications_courses),
                type = NotificationType.COURSE_RECOMMENDATION,
                settings = uiState.notifications,
                onChange = onNotificationChange,
            )
            NotificationRow(
                label = stringResource(R.string.settings_notifications_jobs),
                type = NotificationType.JOB_ALERTS,
                settings = uiState.notifications,
                onChange = onNotificationChange,
            )
        }

        SettingsCard(
            title = stringResource(R.string.settings_data_title),
            subtitle = stringResource(R.string.settings_data_subtitle),
        ) {
            SettingsRow(label = stringResource(R.string.settings_data_download))
            SettingsRow(
                label = stringResource(R.string.settings_data_delete),
                isDestructive = true,
            )
            SettingsRow(label = stringResource(R.string.settings_data_privacy))
            SettingsRow(label = stringResource(R.string.settings_data_terms))
        }

        // Preferencias del sistema. El tema sí funciona y se guarda en el dispositivo.
        SettingsCard(
            title = stringResource(R.string.settings_system_title),
            subtitle = stringResource(R.string.settings_system_subtitle),
        ) {
            Text(
                text = stringResource(R.string.settings_theme_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(
                text = stringResource(R.string.settings_theme_description),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ThemePreference.entries.forEach { preference ->
                ThemeOptionRow(
                    label = stringResource(preference.labelRes()),
                    selected = preference == uiState.themePreference,
                    onSelect = { onThemeSelected(preference) },
                )
            }

            // Idioma y zona horaria van fijos: la app solo está en español y no hay ajuste
            // de zona horaria en la Fase I.
            ReadOnlyPreference(
                label = stringResource(R.string.settings_system_language),
                value = stringResource(R.string.settings_system_language_value),
            )
            ReadOnlyPreference(
                label = stringResource(R.string.settings_system_timezone),
                value = stringResource(R.string.settings_system_timezone_value),
            )
        }

        HelpCard(
            onEmailClick = {
                val email = Intent(Intent.ACTION_SENDTO, "mailto:".toUri()).apply {
                    putExtra(
                        Intent.EXTRA_SUBJECT,
                        context.getString(R.string.settings_help_email_subject),
                    )
                }
                context.startActivity(email)
            },
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

/** Tarjeta blanca de un bloque de ajustes. */
@Composable
private fun SettingsCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            content()
        }
    }
}

/**
 * Una fila de ajuste: el cuadrito de color, el nombre y la flecha.
 *
 * TODO: ninguna tiene acción todavía. Faltan las pantallas y los endpoints detrás.
 */
@Composable
private fun SettingsRow(
    label: String,
    modifier: Modifier = Modifier,
    isDestructive: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val accent = if (isDestructive) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            // Las filas sin acción todavía no llevan a ninguna parte: no responden al toque
            // para no prometer una pantalla que no existe.
            .clickable(enabled = onClick != null) { onClick?.invoke() },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(accent),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isDestructive) accent else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp),
            )
            Text(
                text = "›",
                style = MaterialTheme.typography.titleMedium,
                color = accent,
            )
        }
    }
}

/**
 * Una fila de aviso, con su interruptor.
 *
 * Toda la fila responde al toque, no solo el interruptor: es más cómodo en un teléfono y
 * evita tener que apuntarle a un blanco chico.
 */
@Composable
private fun NotificationRow(
    label: String,
    type: NotificationType,
    settings: NotificationSettings,
    onChange: (NotificationType, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isEnabled = settings.isEnabled(type)

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .clickable { onChange(type, !isEnabled) },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.primary),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp),
            )
            Switch(
                checked = isEnabled,
                onCheckedChange = { onChange(type, it) },
            )
        }
    }
}

/** Preferencia que se muestra pero no se puede cambiar en esta fase. */
@Composable
private fun ReadOnlyPreference(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(top = 12.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(10.dp),
            )
        }
    }
}

/** Tarjeta oscura de soporte, al pie del diseño. */
@Composable
private fun HelpCard(
    onEmailClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.settings_help_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.surface,
            )
            Text(
                text = stringResource(R.string.settings_help_subtitle),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.surface,
            )
            Button(
                onClick = onEmailClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            ) {
                Text(stringResource(R.string.settings_help_email))
            }
        }
    }
}

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
            .padding(vertical = 6.dp),
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

private fun ThemePreference.labelRes(): Int = when (this) {
    ThemePreference.SYSTEM -> R.string.settings_theme_system
    ThemePreference.LIGHT -> R.string.settings_theme_light
    ThemePreference.DARK -> R.string.settings_theme_dark
}

@Preview(name = "Configuración · claro", widthDp = 390, heightDp = 1300)
@Composable
private fun SettingsPreviewLight() {
    CvProTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            SettingsContent(
                uiState = SettingsUiState(ThemePreference.LIGHT),
                onThemeSelected = {},
                onNotificationChange = { _, _ -> },
            )
        }
    }
}

@Preview(name = "Configuración · oscuro", widthDp = 390, heightDp = 1300)
@Composable
private fun SettingsPreviewDark() {
    CvProTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            SettingsContent(
                uiState = SettingsUiState(ThemePreference.DARK),
                onThemeSelected = {},
                onNotificationChange = { _, _ -> },
            )
        }
    }
}
