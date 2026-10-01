package com.cvpro.app.ui.screens.templates

import android.content.Intent
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.cvpro.app.R
import com.cvpro.app.domain.model.CvTemplate
import com.cvpro.app.ui.components.EmptyState
import com.cvpro.app.ui.components.LocalSnackbarHostState
import com.cvpro.app.ui.components.outlinedOnCardColors
import com.cvpro.app.ui.theme.CvProTheme

/** Plantillas sin conexión con estado. */
@Composable
fun TemplatesScreen(
    modifier: Modifier = Modifier,
    viewModel: TemplatesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val downloadedPreview by viewModel.downloadedPreview.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // El Scaffold de la raíz comparte un único snackbar: la pantalla lo usa en vez de
    // montar su propio Scaffold, que duplicaría el fondo y los insets.
    val snackbarHostState = LocalSnackbarHostState.current

    // Apenas la vista previa está guardada se abre con el visor de imágenes del teléfono.
    LaunchedEffect(downloadedPreview) {
        val uri = downloadedPreview ?: return@LaunchedEffect
        val abrir = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "image/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { context.startActivity(abrir) }
        viewModel.onPreviewOpened()
    }

    // El resultado de usar o descargar una plantilla se avisa abajo y se limpia solo.
    LaunchedEffect(uiState.message) {
        val mensaje = uiState.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(mensaje)
        viewModel.onMessageShown()
    }

    TemplatesContent(
        uiState = uiState,
        onRetry = viewModel::retry,
        onUseTemplate = viewModel::onUseTemplate,
        onDownloadTemplate = viewModel::onDownloadTemplate,
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
    onUseTemplate: (CvTemplate) -> Unit,
    onDownloadTemplate: (CvTemplate) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Cada sección arranca mostrando solo las primeras y se despliega con "Ver todas".
    var offlineExpanded by remember { mutableStateOf(false) }
    var storeExpanded by remember { mutableStateOf(false) }

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
                        items(
                            items = offline.shown(offlineExpanded),
                            key = { it.id },
                        ) { template ->
                            TemplateCard(
                                template = template,
                                isInStore = false,
                                isBusy = uiState.busyTemplateId == template.id,
                                onActionClick = { onUseTemplate(template) },
                            )
                        }
                        if (offline.size > PREVIEW_COUNT) {
                            item {
                                ExpandButton(
                                    isExpanded = offlineExpanded,
                                    hiddenCount = offline.size - PREVIEW_COUNT,
                                    onClick = { offlineExpanded = !offlineExpanded },
                                )
                            }
                        }
                    }

                    if (store.isNotEmpty()) {
                        item {
                            SectionTitle(
                                text = stringResource(R.string.templates_store_section),
                                count = store.size,
                            )
                        }
                        items(
                            items = store.shown(storeExpanded),
                            key = { it.id },
                        ) { template ->
                            TemplateCard(
                                template = template,
                                isInStore = true,
                                isBusy = uiState.busyTemplateId == template.id,
                                onActionClick = { onDownloadTemplate(template) },
                            )
                        }
                        if (store.size > PREVIEW_COUNT) {
                            item {
                                ExpandButton(
                                    isExpanded = storeExpanded,
                                    hiddenCount = store.size - PREVIEW_COUNT,
                                    onClick = { storeExpanded = !storeExpanded },
                                )
                            }
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
    isBusy: Boolean,
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
                    // Mientras la plantilla está trabajando el botón queda apagado, para no
                    // disparar dos descargas o dos currículums con un doble toque.
                    enabled = !isBusy,
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
                            when {
                                isBusy && isInStore -> R.string.templates_downloading
                                isBusy -> R.string.templates_creating
                                isInStore -> R.string.templates_download
                                else -> R.string.templates_use
                            },
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
                onUseTemplate = {},
                onDownloadTemplate = {},
            )
        }
    }
}

/**
 * Botón para ver el resto de una sección recortada.
 *
 * Dice cuántas faltan en vez de un "Ver todas" a secas, igual que en Análisis de Brecha:
 * así se sabe si vale la pena abrirlo antes de tocarlo.
 */
@Composable
private fun ExpandButton(
    isExpanded: Boolean,
    hiddenCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        colors = outlinedOnCardColors(),
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = if (isExpanded) {
                stringResource(R.string.templates_show_less)
            } else {
                stringResource(R.string.templates_show_all, hiddenCount)
            },
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

/** Las primeras PREVIEW_COUNT, o todas si la sección está desplegada. */
private fun List<CvTemplate>.shown(isExpanded: Boolean): List<CvTemplate> =
    if (isExpanded) this else take(PREVIEW_COUNT)

/** Cuántas plantillas se ven en cada sección antes de tocar "Ver todas". */
private const val PREVIEW_COUNT = 3
