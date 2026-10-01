package com.cvpro.app.ui.screens.templates

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.cvpro.app.R
import com.cvpro.app.domain.model.CvTemplate
import com.cvpro.app.ui.components.ComingSoonSheet
import com.cvpro.app.ui.components.EmptyState
import com.cvpro.app.ui.theme.CvProTheme

/** Plantillas sin conexión con estado. */
@Composable
fun TemplatesScreen(
    modifier: Modifier = Modifier,
    viewModel: TemplatesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    TemplatesContent(
        uiState = uiState,
        onRetry = viewModel::retry,
        modifier = modifier,
    )
}

/**
 * Catálogo de plantillas, siguiendo el diseño de Samuel.
 *
 * Cada plantilla es una tarjeta con su vista previa a la izquierda y el nombre, la
 * descripción y el botón de uso a la derecha.
 */
@Composable
fun TemplatesContent(
    uiState: TemplatesUiState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Qué aviso mostrar: usar una plantilla o descargarla. Los dos necesitan el editor o
    // el servicio de la Fase II, pero conviene que cada botón diga lo suyo.
    var aviso by remember { mutableStateOf<TemplateNotice?>(null) }

    aviso?.let { actual ->
        ComingSoonSheet(
            title = stringResource(actual.titleRes),
            message = stringResource(actual.messageRes),
            onDismiss = { aviso = null },
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        TemplatesHeader(count = uiState.templates.size)

        when {
            uiState.isLoading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            uiState.error != null -> TemplatesError(message = uiState.error, onRetry = onRetry)

            uiState.templates.isEmpty() -> EmptyState(
                title = stringResource(R.string.templates_empty_title),
                message = stringResource(R.string.templates_empty_message),
            )

            else -> {
                // El contrato marca con availableOffline las plantillas que ya vienen con
                // la app. Las demás son de la tienda: hay que descargarlas para usarlas.
                val offline = uiState.templates.filter { it.availableOffline }
                val store = uiState.templates.filterNot { it.availableOffline }

                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (offline.isNotEmpty()) {
                        item {
                            SectionTitle(
                                text = stringResource(R.string.templates_offline_section),
                                count = offline.size,
                            )
                        }
                        items(items = offline, key = { it.id }) { template ->
                            TemplateCard(
                                template = template,
                                isInStore = false,
                                onActionClick = { aviso = TemplateNotice.USE },
                            )
                        }
                    }

                    if (store.isNotEmpty()) {
                        item {
                            SectionTitle(
                                text = stringResource(R.string.templates_store_section),
                                count = store.size,
                            )
                        }
                        items(items = store, key = { it.id }) { template ->
                            TemplateCard(
                                template = template,
                                isInStore = true,
                                onActionClick = { aviso = TemplateNotice.DOWNLOAD },
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Antetítulo, título y el acceso al catálogo completo. */
@Composable
private fun TemplatesHeader(
    count: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.End,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(50),
        ) {
            Text(
                text = stringResource(R.string.templates_count, count),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(
                    horizontal = 10.dp,
                    vertical = 5.dp,
                ),
            )
        }
    }
}

/**
 * Tarjeta de una plantilla.
 *
 * A diferencia de los currículums, acá la vista previa sí es una imagen real: el contrato
 * define previewUrl para las plantillas.
 */
@Composable
private fun SectionTitle(
    text: String,
    count: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .size(width = 4.dp, height = 18.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.primary),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
        )
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(50),
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            )
        }
    }
}

/**
 * Tarjeta de una plantilla.
 *
 * @param isInStore true si hay que descargarla antes de usarla; false si ya viene con la
 * app y se puede usar sin conexión.
 */
@Composable
private fun TemplateCard(
    template: CvTemplate,
    isInStore: Boolean,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(10.dp)) {
            TemplatePreview(previewUrl = template.previewUrl)

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = template.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (template.isPremium) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(50),
                        ) {
                            Text(
                                text = stringResource(R.string.templates_premium),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                    }
                }

                Text(
                    text = template.description,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )

                Button(
                    onClick = onActionClick,
                    colors = if (isInStore) {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onSurface,
                            contentColor = MaterialTheme.colorScheme.surface,
                        )
                    } else {
                        ButtonDefaults.buttonColors()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                ) {
                    Text(
                        text = stringResource(
                            if (isInStore) R.string.templates_download else R.string.templates_use,
                        ),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}

/** Vista previa de la plantilla, con su etiqueta encima como en el diseño. */
@Composable
private fun TemplatePreview(
    previewUrl: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .width(110.dp)
            .height(150.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        AsyncImage(
            model = previewUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(6.dp),
        ) {
            Text(
                text = stringResource(R.string.templates_preview),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            )
        }
    }
}

@Composable
private fun TemplatesError(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.templates_error_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onRetry) {
                Text(stringResource(R.string.applications_retry))
            }
        }
    }
}

@Preview(name = "Plantillas · claro", widthDp = 390, heightDp = 900)
@Composable
private fun TemplatesPreviewLight() = TemplatesPreview(darkTheme = false)

@Preview(name = "Plantillas · oscuro", widthDp = 390, heightDp = 900)
@Composable
private fun TemplatesPreviewDark() = TemplatesPreview(darkTheme = true)

@Composable
private fun TemplatesPreview(darkTheme: Boolean) {
    CvProTheme(darkTheme = darkTheme) {
        Surface(color = MaterialTheme.colorScheme.background) {
            TemplatesContent(
                uiState = TemplatesUiState(
                    templates = listOf(
                        CvTemplate(
                            id = "1",
                            name = "Clásica",
                            description = "Equilibrada y profesional",
                            previewUrl = "",
                            category = "General",
                            isPremium = false,
                            availableOffline = true,
                        ),
                        CvTemplate(
                            id = "2",
                            name = "Moderna",
                            description = "Visual y orientada a datos",
                            previewUrl = "",
                            category = "General",
                            isPremium = true,
                            availableOffline = false,
                        ),
                    ),
                ),
                onRetry = {},
            )
        }
    }
}

/** Los dos avisos de la pantalla de plantillas. */
private enum class TemplateNotice(
    @param:StringRes val titleRes: Int,
    @param:StringRes val messageRes: Int,
) {
    USE(R.string.coming_soon_use_title, R.string.coming_soon_use_message),
    DOWNLOAD(R.string.coming_soon_template_title, R.string.coming_soon_template_message),
}
