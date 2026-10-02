package com.cvpro.app.ui.screens.settings

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cvpro.app.R
import com.cvpro.app.domain.model.HelpContact
import com.cvpro.app.domain.model.ActiveSession
import com.cvpro.app.domain.model.LegalDocument
import com.cvpro.app.domain.model.ThemePreference
import com.cvpro.app.domain.repository.NotificationSettings
import com.cvpro.app.domain.repository.NotificationType
import com.cvpro.app.ui.theme.CvProTheme

private enum class SettingsDialog {
    SESSIONS,
    PRIVACY,
    TERMS,
    DELETE_ACCOUNT,
}

/**
 * Pantalla principal de configuración.
 *
 * Las preferencias locales, como el tema, se manejan desde SettingsRepository.
 * Los datos que pueden cambiar, como sesiones, documentos legales y exportación,
 * se consultan al APIM mediante SettingsContentRepository.
 */
@Composable
fun SettingsScreen(
    onSignOutClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Los textos se obtienen dentro del Composable y luego se reutilizan
    // dentro del callback del selector de documentos.
    val exportSuccessMessage =
        stringResource(R.string.settings_export_success)

    val exportErrorMessage =
        stringResource(R.string.settings_export_error)

    var pendingExportContent by remember {
        mutableStateOf<String?>(null)
    }

    /*
     * Abre el selector nativo de Android para que el usuario elija
     * dónde guardar el JSON recibido desde el API.
     */
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        val content = pendingExportContent

        if (uri != null && content != null) {
            val result = runCatching {
                context.contentResolver
                    .openOutputStream(uri)
                    ?.use { outputStream ->
                        outputStream.write(content.toByteArray())
                    }
                    ?: error("No se pudo abrir el archivo.")
            }

            Toast.makeText(
                context,
                if (result.isSuccess) {
                    exportSuccessMessage
                } else {
                    exportErrorMessage
                },
                Toast.LENGTH_SHORT,
            ).show()
        }

        pendingExportContent = null
        viewModel.onExportConsumed()
    }

    /*
     * Cuando el ViewModel termina la consulta al endpoint de exportación,
     * abre automáticamente el selector de archivos.
     */
    LaunchedEffect(uiState.exportContent) {
        val content =
            uiState.exportContent ?: return@LaunchedEffect

        pendingExportContent = content

        createDocumentLauncher.launch(
            "cvexpert-data.json"
        )
    }

    SettingsContent(
        uiState = uiState,
        onThemeSelected = viewModel::onThemeSelected,
        onNotificationChange = viewModel::onNotificationChange,
        onSessionsClick = viewModel::loadSessions,
        onPrivacyClick = viewModel::loadPrivacyPolicy,
        onTermsClick = viewModel::loadTerms,
        onExportClick = viewModel::requestDataExport,
        onSignOutClick = onSignOutClick,
        modifier = modifier,
    )
}

/**
 * Contenido visual de la pantalla.
 */
@Composable
fun SettingsContent(
    uiState: SettingsUiState,
    onThemeSelected: (ThemePreference) -> Unit,
    onNotificationChange: (NotificationType, Boolean) -> Unit,
    onSessionsClick: () -> Unit = {},
    onPrivacyClick: () -> Unit = {},
    onTermsClick: () -> Unit = {},
    onExportClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    onSignOutClick: () -> Unit = {},
) {
    var openDialog by remember {
        mutableStateOf<SettingsDialog?>(null)
    }

    val context = LocalContext.current
    val emailSubject =
        stringResource(R.string.settings_help_email_subject)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = 12.dp,
                vertical = 8.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {

        // Seguridad
        SettingsCard(
            title = stringResource(
                R.string.settings_security_title
            ),
            subtitle = stringResource(
                R.string.settings_security_subtitle
            ),
        ) {
            SettingsRow(
                label = stringResource(
                    R.string.settings_security_sessions
                ),
                icon = Icons.Default.Devices,
                onClick = {
                    openDialog = SettingsDialog.SESSIONS
                    onSessionsClick()
                },
            )

            SettingsRow(
                label = stringResource(
                    R.string.home_sign_out
                ),
                icon = Icons.Default.ExitToApp,
                onClick = onSignOutClick,
            )
        }

        // Notificaciones
        SettingsCard(
            title = stringResource(
                R.string.settings_notifications_title
            ),
            subtitle = stringResource(
                R.string.settings_notifications_subtitle
            ),
        ) {
            NotificationRow(
                label = stringResource(
                    R.string.settings_notifications_applications
                ),
                icon = Icons.Default.Notifications,
                type = NotificationType.APPLICATION_STATUS,
                settings = uiState.notifications,
                onChange = onNotificationChange,
            )

            NotificationRow(
                label = stringResource(
                    R.string.settings_notifications_courses
                ),
                icon = Icons.Default.School,
                type = NotificationType.COURSE_RECOMMENDATION,
                settings = uiState.notifications,
                onChange = onNotificationChange,
            )

            NotificationRow(
                label = stringResource(
                    R.string.settings_notifications_jobs
                ),
                icon = Icons.Default.Work,
                type = NotificationType.JOB_ALERTS,
                settings = uiState.notifications,
                onChange = onNotificationChange,
            )
        }

        // Datos
        SettingsCard(
            title = stringResource(
                R.string.settings_data_title
            ),
            subtitle = stringResource(
                R.string.settings_data_subtitle
            ),
        ) {
            SettingsRow(
                label = if (uiState.isExporting) {
                    stringResource(
                        R.string.settings_data_exporting
                    )
                } else {
                    stringResource(
                        R.string.settings_data_download
                    )
                },
                icon = Icons.Default.Download,
                enabled = !uiState.isExporting,
                onClick = onExportClick,
            )

            SettingsRow(
                label = stringResource(
                    R.string.settings_data_delete
                ),
                icon = Icons.Default.Delete,
                isDestructive = true,
                onClick = {
                    openDialog =
                        SettingsDialog.DELETE_ACCOUNT
                },
            )

            SettingsRow(
                label = stringResource(
                    R.string.settings_data_privacy
                ),
                icon = Icons.Default.Security,
                onClick = {
                    openDialog =
                        SettingsDialog.PRIVACY

                    onPrivacyClick()
                },
            )

            SettingsRow(
                label = stringResource(
                    R.string.settings_data_terms
                ),
                icon = Icons.Default.Description,
                onClick = {
                    openDialog =
                        SettingsDialog.TERMS

                    onTermsClick()
                },
            )
        }

        // Preferencias del sistema
        SettingsCard(
            title = stringResource(
                R.string.settings_system_title
            ),
            subtitle = stringResource(
                R.string.settings_system_subtitle
            ),
        ) {
            Text(
                text = stringResource(
                    R.string.settings_theme_title
                ),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 12.dp),
            )

            Text(
                text = stringResource(
                    R.string.settings_theme_description
                ),
                style = MaterialTheme.typography.labelSmall,
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant,
            )

            ThemePreference.entries.forEach { preference ->
                ThemeOptionRow(
                    label = stringResource(
                        preference.labelRes()
                    ),
                    selected =
                        preference == uiState.themePreference,
                    onSelect = {
                        onThemeSelected(preference)
                    },
                )
            }

            /*
             * Estos valores sí pueden permanecer fijos porque actualmente
             * la aplicación solo maneja español y la zona horaria definida.
             */
            ReadOnlyPreference(
                label = stringResource(
                    R.string.settings_system_language
                ),
                value = stringResource(
                    R.string.settings_system_language_value
                ),
            )

            ReadOnlyPreference(
                label = stringResource(
                    R.string.settings_system_timezone
                ),
                value = stringResource(
                    R.string.settings_system_timezone_value
                ),
            )
        }

        // Ayuda
        HelpCard(
            contacts = uiState.helpContacts,
            onEmailClick = { address ->
                // Con dirección, el correo se abre ya dirigido a ese canal; sin ella, se
                // abre vacío y el usuario elige a quién escribirle.
                val destino = if (address.isNullOrBlank()) {
                    "mailto:"
                } else {
                    "mailto:$address"
                }

                val email = Intent(
                    Intent.ACTION_SENDTO,
                    destino.toUri(),
                ).apply {
                    putExtra(
                        Intent.EXTRA_SUBJECT,
                        emailSubject,
                    )
                }

                // Si el teléfono no tiene app de correo, startActivity lanza excepción y
                // tumbaría la app. Mejor que el botón no haga nada a que se cierre.
                runCatching { context.startActivity(email) }
            },
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )
    }

    /*
     * Los diálogos se muestran fuera del Column.
     * Así no forman parte del scroll de la pantalla.
     */
    when (openDialog) {

        SettingsDialog.SESSIONS -> {
            SessionsDialog(
                sessions = uiState.sessions,
                isLoading = uiState.isLoadingSessions,
                error = uiState.error,
                onDismiss = {
                    openDialog = null
                },
            )
        }

        SettingsDialog.PRIVACY -> {
            LegalDocumentDialog(
                fallbackTitle = stringResource(
                    R.string.settings_data_privacy
                ),
                document = uiState.privacyPolicy,
                isLoading = uiState.isLoadingLegal,
                error = uiState.error,
                onDismiss = {
                    openDialog = null
                },
            )
        }

        SettingsDialog.TERMS -> {
            LegalDocumentDialog(
                fallbackTitle = stringResource(
                    R.string.settings_data_terms
                ),
                document = uiState.terms,
                isLoading = uiState.isLoadingLegal,
                error = uiState.error,
                onDismiss = {
                    openDialog = null
                },
            )
        }

        SettingsDialog.DELETE_ACCOUNT -> {
            DeleteAccountDialog(
                onDismiss = {
                    openDialog = null
                },
                onConfirm = {
                    /*
                     * La confirmación existe porque es una acción
                     * destructiva. En esta fase no ejecutamos DELETE.
                     */
                    openDialog = null
                },
            )
        }

        null -> Unit
    }
}

/**
 * Tarjeta general de Configuración.
 */
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
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = title,
                style =
                    MaterialTheme.typography.titleMedium,
                color =
                    MaterialTheme.colorScheme.onSurface,
            )

            Text(
                text = subtitle,
                style =
                    MaterialTheme.typography.labelMedium,
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant,
            )

            content()
        }
    }
}

/**
 * Fila estándar de configuración.
 *
 * La flecha solo aparece cuando la fila realmente tiene una acción.
 */
@Composable
private fun SettingsRow(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    isDestructive: Boolean = false,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val accent =
        if (isDestructive) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.primary
        }

    Surface(
        color =
            MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .heightIn(min = 48.dp)
            .clickable(
                enabled = enabled && onClick != null,
            ) {
                onClick?.invoke()
            },
    ) {
        Row(
            verticalAlignment =
                Alignment.CenterVertically,
            modifier = Modifier.padding(10.dp),
        ) {

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(
                        RoundedCornerShape(7.dp)
                    )
                    .background(
                        brush =
                            if (isDestructive) {
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.error,
                                        MaterialTheme.colorScheme.error,
                                    )
                                )
                            } else {
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFF4DA3FF),
                                        Color(0xFF1565F5),
                                    )
                                )
                            }
                    ),
                contentAlignment =
                    Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }

            Text(
                text = label,
                style =
                    MaterialTheme.typography.bodyMedium,
                color =
                    if (enabled) {
                        if (isDestructive) {
                            accent
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    } else {
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                    },
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp),
            )

            if (
                enabled &&
                onClick != null
            ) {
                Icon(
                    imageVector =
                        Icons.AutoMirrored.Filled
                            .KeyboardArrowRight,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

/**
 * Diálogo de sesiones activas.
 */
@Composable
private fun SessionsDialog(
    sessions: List<ActiveSession>,
    isLoading: Boolean,
    error: String?,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,

        title = {
            Text(
                text = stringResource(
                    R.string.settings_sessions_title
                )
            )
        },

        text = {
            when {

                isLoading -> {
                    Box(
                        modifier =
                            Modifier.fillMaxWidth(),
                        contentAlignment =
                            Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                error != null -> {
                    Text(
                        text = stringResource(
                            R.string.settings_load_error
                        ),
                        color =
                            MaterialTheme.colorScheme.error,
                    )
                }

                sessions.isEmpty() -> {
                    Text(
                        text = stringResource(
                            R.string.settings_sessions_empty
                        )
                    )
                }

                else -> {
                    Column(
                        verticalArrangement =
                            Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp)
                            .verticalScroll(
                                rememberScrollState()
                            ),
                    ) {
                        sessions.forEachIndexed {
                                index,
                                session ->

                            SessionItem(
                                session = session
                            )

                            if (
                                index <
                                sessions.lastIndex
                            ) {
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        },

        confirmButton = {
            TextButton(
                onClick = onDismiss,
            ) {
                Text(
                    text = stringResource(
                        R.string.settings_close
                    )
                )
            }
        },
    )
}

/**
 * Una sesión del usuario.
 */
@Composable
private fun SessionItem(
    session: ActiveSession,
) {
    Column(
        verticalArrangement =
            Arrangement.spacedBy(3.dp),
    ) {
        Row(
            verticalAlignment =
                Alignment.CenterVertically,
        ) {
            Text(
                text = session.deviceName,
                style =
                    MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )

            if (session.current) {
                Surface(
                    color =
                        MaterialTheme.colorScheme
                            .primaryContainer,
                    shape = RoundedCornerShape(50),
                ) {
                    Text(
                        text = stringResource(
                            R.string
                                .settings_session_current
                        ),
                        style =
                            MaterialTheme.typography
                                .labelSmall,
                        color =
                            MaterialTheme.colorScheme
                                .primary,
                        modifier =
                            Modifier.padding(
                                horizontal = 8.dp,
                                vertical = 3.dp,
                            ),
                    )
                }
            }
        }

        Text(
            text = session.client,
            style =
                MaterialTheme.typography.bodyMedium,
        )

        Text(
            text = session.platform,
            style =
                MaterialTheme.typography.labelMedium,
            color =
                MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(
            text = stringResource(
                R.string.settings_session_last_active,
                session.lastActiveAt,
            ),
            style =
                MaterialTheme.typography.labelSmall,
            color =
                MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Diálogo reutilizado por Privacidad y Términos.
 *
 * El contenido principal viene del API.
 */
@Composable
private fun LegalDocumentDialog(
    fallbackTitle: String,
    document: LegalDocument?,
    isLoading: Boolean,
    error: String?,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,

        title = {
            Text(
                text =
                    document?.title
                        ?: fallbackTitle
            )
        },

        text = {
            when {

                isLoading -> {
                    Box(
                        modifier =
                            Modifier.fillMaxWidth(),
                        contentAlignment =
                            Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                error != null -> {
                    Text(
                        text = stringResource(
                            R.string.settings_load_error
                        ),
                        color =
                            MaterialTheme.colorScheme.error,
                    )
                }

                document != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 450.dp)
                            .verticalScroll(
                                rememberScrollState()
                            ),
                    ) {
                        Text(
                            text = stringResource(
                                R.string
                                    .settings_document_version,
                                document.version,
                            ),
                            style =
                                MaterialTheme.typography
                                    .labelMedium,
                            color =
                                MaterialTheme.colorScheme
                                    .primary,
                        )

                        Text(
                            text = stringResource(
                                R.string
                                    .settings_document_effective,
                                document.effectiveDate,
                            ),
                            style =
                                MaterialTheme.typography
                                    .labelSmall,
                            color =
                                MaterialTheme.colorScheme
                                    .onSurfaceVariant,
                            modifier =
                                Modifier.padding(
                                    top = 2.dp,
                                    bottom = 12.dp,
                                ),
                        )

                        Text(
                            text = document.content,
                            style =
                                MaterialTheme.typography
                                    .bodyMedium,
                        )
                    }
                }

                else -> {
                    Text(
                        text = stringResource(
                            R.string.settings_load_error
                        )
                    )
                }
            }
        },

        confirmButton = {
            TextButton(
                onClick = onDismiss,
            ) {
                Text(
                    text = stringResource(
                        R.string.settings_close
                    )
                )
            }
        },
    )
}

/**
 * Confirmación para una acción destructiva.
 *
 * En esta fase la confirmación no ejecuta un DELETE real.
 */
@Composable
private fun DeleteAccountDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,

        title = {
            Text(
                text = stringResource(
                    R.string.settings_delete_account_title
                )
            )
        },

        text = {
            Text(
                text = stringResource(
                    R.string.settings_delete_account_message
                )
            )
        },

        dismissButton = {
            TextButton(
                onClick = onDismiss,
            ) {
                Text(
                    text = stringResource(
                        R.string.settings_cancel
                    )
                )
            }
        },

        confirmButton = {
            TextButton(
                onClick = onConfirm,
            ) {
                Text(
                    text = stringResource(
                        R.string
                            .settings_delete_account_confirm
                    ),
                    color =
                        MaterialTheme.colorScheme.error,
                )
            }
        },
    )
}

/**
 * Preferencia de notificación.
 */
@Composable
private fun NotificationRow(
    label: String,
    icon: ImageVector,
    type: NotificationType,
    settings: NotificationSettings,
    onChange: (NotificationType, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isEnabled =
        settings.isEnabled(type)

    Surface(
        color =
            MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .heightIn(min = 48.dp)
            .clickable {
                onChange(
                    type,
                    !isEnabled,
                )
            },
    ) {
        Row(
            verticalAlignment =
                Alignment.CenterVertically,
            modifier = Modifier.padding(
                start = 10.dp,
                end = 10.dp,
                top = 4.dp,
                bottom = 4.dp,
            ),
        ) {

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(
                        RoundedCornerShape(7.dp)
                    )
                    .background(
                        brush =
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF4DA3FF),
                                    Color(0xFF1565F5),
                                )
                            )
                    ),
                contentAlignment =
                    Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }

            Text(
                text = label,
                style =
                    MaterialTheme.typography.bodyMedium,
                color =
                    MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp),
            )

            Switch(
                checked = isEnabled,
                onCheckedChange = {
                    onChange(
                        type,
                        it,
                    )
                },
            )
        }
    }
}

/**
 * Preferencia informativa que no puede modificarse.
 */
@Composable
private fun ReadOnlyPreference(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier.padding(top = 12.dp)
    ) {
        Text(
            text = label,
            style =
                MaterialTheme.typography.labelSmall,
            color =
                MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Surface(
            color =
                MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
        ) {
            Text(
                text = value,
                style =
                    MaterialTheme.typography.bodyMedium,
                color =
                    MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(10.dp),
            )
        }
    }
}

/**
 * Tarjeta de soporte.
 */
@Composable
private fun HelpCard(
    contacts: List<HelpContact>,
    onEmailClick: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color =
            MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = stringResource(
                    R.string.settings_help_title
                ),
                style =
                    MaterialTheme.typography.titleMedium,
                color =
                    MaterialTheme.colorScheme.surface,
            )

            Text(
                text = stringResource(
                    R.string.settings_help_subtitle
                ),
                style =
                    MaterialTheme.typography.labelMedium,
                color =
                    MaterialTheme.colorScheme.surface,
            )

            // Con los canales del servicio se muestra un botón por cada uno, con su
            // etiqueta. Si el servicio no respondió queda el botón de antes, que abre el
            // correo sin destinatario: así la tarjeta nunca se queda sin salida.
            if (contacts.isEmpty()) {
                Button(
                    onClick = { onEmailClick(null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                ) {
                    Text(
                        text = stringResource(
                            R.string.settings_help_email
                        )
                    )
                }
            } else {
                contacts.forEach { contact ->
                    Button(
                        onClick = { onEmailClick(contact.email) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = contact.label,
                                style =
                                    MaterialTheme.typography.labelLarge,
                            )
                            Text(
                                text = contact.email,
                                style =
                                    MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Opción de tema.
 */
@Composable
private fun ThemeOptionRow(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment =
            Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(
                selected = selected,
                onClick = onSelect,
                role = Role.RadioButton,
            )
            .padding(vertical = 6.dp),
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
        )

        Text(
            text = label,
            style =
                MaterialTheme.typography.bodyMedium,
            color =
                MaterialTheme.colorScheme.onSurface,
            modifier =
                Modifier.padding(start = 8.dp),
        )
    }
}

private fun ThemePreference.labelRes(): Int =
    when (this) {
        ThemePreference.SYSTEM ->
            R.string.settings_theme_system

        ThemePreference.LIGHT ->
            R.string.settings_theme_light

        ThemePreference.DARK ->
            R.string.settings_theme_dark
    }

@Preview(
    name = "Configuración · claro",
    widthDp = 390,
    heightDp = 1300,
)
@Composable
private fun SettingsPreviewLight() {
    CvProTheme(
        darkTheme = false
    ) {
        Surface(
            color =
                MaterialTheme.colorScheme.background
        ) {
            SettingsContent(
                uiState = SettingsUiState(
                    themePreference =
                        ThemePreference.LIGHT
                ),
                onThemeSelected = {},
                onNotificationChange = { _, _ -> },
            )
        }
    }
}

@Preview(
    name = "Configuración · oscuro",
    widthDp = 390,
    heightDp = 1300,
)
@Composable
private fun SettingsPreviewDark() {
    CvProTheme(
        darkTheme = true
    ) {
        Surface(
            color =
                MaterialTheme.colorScheme.background
        ) {
            SettingsContent(
                uiState = SettingsUiState(
                    themePreference =
                        ThemePreference.DARK
                ),
                onThemeSelected = {},
                onNotificationChange = { _, _ -> },
            )
        }
    }
}