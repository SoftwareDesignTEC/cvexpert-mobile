package com.cvpro.app.ui.screens.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.ViewKanban
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cvpro.app.R
import com.cvpro.app.domain.model.AppNotification
import com.cvpro.app.domain.model.NotificationKind
import com.cvpro.app.ui.components.EmptyState
import com.cvpro.app.ui.components.ErrorState
import com.cvpro.app.ui.components.LoadingState
import com.cvpro.app.ui.components.SecondaryButton
import com.cvpro.app.ui.components.toLocalDateTime

/**
 * Centro de notificaciones, a pantalla completa sobre la pantalla actual.
 *
 * Es un diálogo por lo mismo que Alertas de ofertas: la barra de la app toma su título del menú
 * lateral y no tiene flecha de volver. Trae su propio SnackbarHost porque el de la app queda
 * detrás del diálogo.
 *
 * @param onDismiss se llama al cerrar; quien lo abrió aprovecha para actualizar la insignia.
 */
@Composable
fun NotificationsDialog(
    onDismiss: () -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Mismo patrón que Perfil y Alertas: el efecto guarda qué mensaje mostrar y el texto se
    // resuelve con stringResource, así sigue el idioma del teléfono.
    var pendingMessage by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    // El ViewModel sobrevive al cerrar el diálogo, así que se recarga cada vez que se abre;
    // si no, al volver se verían los datos de la vez anterior.
    LaunchedEffect(Unit) { viewModel.load() }

    LaunchedEffect(viewModel) {
        var seq = 0
        viewModel.messages.collect { messageRes ->
            seq++
            pendingMessage = seq to messageRes
        }
    }
    val messageText = pendingMessage?.let { (_, messageRes) -> stringResource(messageRes) }
    LaunchedEffect(pendingMessage) {
        if (messageText != null) snackbarHostState.showSnackbar(messageText)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        NotificationsContent(
            uiState = uiState,
            snackbarHostState = snackbarHostState,
            onBack = onDismiss,
            onRetry = viewModel::load,
            onFilterChange = viewModel::onFilterChange,
            onNotificationClick = viewModel::onNotificationClick,
            onMarkAllRead = viewModel::onMarkAllRead,
            onLoadMore = viewModel::onLoadMore,
        )
    }
}

/** Contenido sin estado: recibe el UiState y avisa cada acción con una lambda. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsContent(
    uiState: NotificationsUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onFilterChange: (Boolean) -> Unit,
    onNotificationClick: (AppNotification) -> Unit,
    onMarkAllRead: () -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.notifications_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.alerts_back),
                        )
                    }
                },
                actions = {
                    // Solo tiene sentido si hay algo sin leer en lo que se ve.
                    if (uiState.hasUnread) {
                        IconButton(onClick = onMarkAllRead, enabled = !uiState.isMarkingAll) {
                            Icon(
                                imageVector = Icons.Outlined.DoneAll,
                                contentDescription = stringResource(R.string.notifications_mark_all),
                            )
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize(),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                FilterChip(
                    selected = !uiState.unreadOnly,
                    onClick = { onFilterChange(false) },
                    label = { Text(stringResource(R.string.notifications_filter_all)) },
                )
                FilterChip(
                    selected = uiState.unreadOnly,
                    onClick = { onFilterChange(true) },
                    label = { Text(stringResource(R.string.notifications_filter_unread)) },
                )
            }

            when {
                uiState.isLoading -> LoadingState()
                uiState.loadFailed -> ErrorState(
                    message = stringResource(R.string.notifications_load_error),
                    onRetryClick = onRetry,
                )
                uiState.notifications.isEmpty() -> EmptyState(
                    title = stringResource(R.string.notifications_empty_title),
                    message = stringResource(
                        if (uiState.unreadOnly) R.string.notifications_empty_unread else R.string.notifications_empty_message,
                    ),
                    icon = Icons.Outlined.NotificationsNone,
                )
                else -> NotificationList(
                    uiState = uiState,
                    onNotificationClick = onNotificationClick,
                    onLoadMore = onLoadMore,
                )
            }
        }
    }
}

@Composable
private fun NotificationList(
    uiState: NotificationsUiState,
    onNotificationClick: (AppNotification) -> Unit,
    onLoadMore: () -> Unit,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 16.dp),
    ) {
        items(uiState.notifications, key = { it.id }) { notification ->
            NotificationRow(notification = notification, onClick = { onNotificationClick(notification) })
        }
        if (uiState.hasMore) {
            item {
                SecondaryButton(
                    text = stringResource(
                        if (uiState.isLoadingMore) R.string.jobs_loading_more else R.string.notifications_load_more,
                    ),
                    onClick = onLoadMore,
                    enabled = !uiState.isLoadingMore,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
        }
    }
}

/** Una notificación. Las no leídas llevan fondo resaltado y un punto, como en cualquier bandeja. */
@Composable
private fun NotificationRow(notification: AppNotification, onClick: () -> Unit) {
    val unreadLabel = stringResource(R.string.notifications_unread_label)
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (notification.read) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            },
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            Icon(
                imageVector = notification.kind.icon(),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            ) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = notification.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Text(
                    text = notification.createdAt.toLocalDateTime(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            if (!notification.read) {
                // El punto solo se ve; al lector de pantalla se le dice "Sin leer".
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .size(10.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .semantics { contentDescription = unreadLabel },
                )
            }
        }
    }
}

private fun NotificationKind.icon(): ImageVector = when (this) {
    NotificationKind.APPLICATION_STATUS -> Icons.Outlined.ViewKanban
    NotificationKind.NEW_JOB -> Icons.Outlined.Work
    NotificationKind.CV_GENERATED -> Icons.Outlined.Description
    NotificationKind.COURSE_RECOMMENDED -> Icons.Outlined.School
    NotificationKind.OTHER -> Icons.Outlined.Notifications
}
