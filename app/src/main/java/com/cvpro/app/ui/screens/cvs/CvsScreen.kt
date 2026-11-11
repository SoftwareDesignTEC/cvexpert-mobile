package com.cvpro.app.ui.screens.cvs

import android.content.Intent
import android.graphics.Bitmap
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cvpro.app.R
import com.cvpro.app.domain.model.Cv
import com.cvpro.app.ui.components.CvEditorSheet
import com.cvpro.app.ui.components.EmptyState
import com.cvpro.app.ui.components.PagePreview
import com.cvpro.app.ui.components.RemoteImage
import com.cvpro.app.ui.components.outlinedOnCardColors
import com.cvpro.app.ui.theme.CvProTheme


/**
 * Vista previa de la tarjeta.
 *
 * Si el servicio manda la imagen del currículum armado, se muestra esa. Si no llega o no
 * carga, se dibuja a mano con los datos del CV, que es lo que se hacía antes.
 */
@Composable
private fun CvPreview(
    cv: Cv,
    page: Bitmap?,
    modifier: Modifier = Modifier,
) {
    // Primero la hoja real dibujada con su plantilla; si todavía no está, lo de antes.
    if (page != null) {
        PagePreview(page = page, modifier = modifier)
        return
    }
    RemoteImage(
        url = cv.previewUrl,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier,
        fallback = { DrawnCvPreview(cv = cv, modifier = modifier) },
    )
}

/** Vista previa dibujada con los datos del CV, para cuando no hay imagen. */
@Composable
private fun DrawnCvPreview(
    cv: Cv,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(
            topStart = 8.dp,
            topEnd = 8.dp,
        ),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
        ) {
            Text(
                text = cv.personalInfo.name,
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFF1F2937),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = cv.personalInfo.email,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF6B7280),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            cv.personalInfo.location
                .takeIf { it.isNotBlank() }
                ?.let { location ->
                    Text(
                        text = location,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF6B7280),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

            Spacer(modifier = Modifier.height(8.dp))

            HorizontalDivider(
                color = Color(0xFFE5E7EB),
            )

            cv.workExperiences.firstOrNull()?.let { experience ->
                Text(
                    text = experience.position,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF1F2937),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp),
                )

                Text(
                    text = experience.company,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF6B7280),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (cv.skills.isNotEmpty()) {
                Text(
                    text = cv.skills
                        .take(3)
                        .joinToString(" · ") { it.name },
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF1565F5),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}
/** Mis Currículums: la lista de CV guardados, con buscador. */
@Composable
fun CvsScreen(
    onTemplatesClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CvsViewModel = hiltViewModel(),
    aiViewModel: CvAiViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val aiState by aiViewModel.uiState.collectAsStateWithLifecycle()
    val generatedPdf by viewModel.generatedPdf.collectAsStateWithLifecycle()
    val savedCv by viewModel.savedCv.collectAsStateWithLifecycle()
    val cvToOpen by viewModel.cvToOpen.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Cada vez que se entra a la pantalla se recarga la lista (ver onScreenShown).
    LaunchedEffect(Unit) { viewModel.onScreenShown() }

    // Apenas el PDF está listo se abre con el visor que tenga el usuario, y se avisa al
    // ViewModel para que no vuelva a abrirse cada vez que la pantalla se repinta.
    LaunchedEffect(generatedPdf) {
        val uri = generatedPdf ?: return@LaunchedEffect
        val abrir = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { context.startActivity(abrir) }
        viewModel.onPdfOpened()
    }

    CvsContent(
        uiState = uiState,
        savedCv = savedCv,
        onQueryChange = viewModel::onQueryChange,
        onRetry = viewModel::retry,
        onDeleteClick = viewModel::deleteCv,
        onDownloadCv = viewModel::onDownloadCv,
        onSaveCv = viewModel::saveCv,
        onSaveHandled = viewModel::onSaveHandled,
        onTemplatesClick = onTemplatesClick,
        cvToOpen = cvToOpen,
        onCvOpened = viewModel::onCvOpened,
        onAiCvCreated = viewModel::onScreenShown,
        aiState = aiState,
        aiActions = CvAiActions(
            onExpand = aiViewModel::expand,
            onTranslate = aiViewModel::translate,
            onResultConsumed = aiViewModel::onResultConsumed,
            onTranslateClosed = aiViewModel::onTranslateClosed,
            onGenerateBio = aiViewModel::generateBio,
            onGenerateDescription = aiViewModel::generateDescription,
        ),
        onEditorChanged = aiViewModel::reset,
        modifier = modifier,
    )
}

/**
 * Pantalla de currículums, siguiendo el diseño de Samuel.
 *
 * Arriba el encabezado con el buscador, después la banda oscura para crear uno nuevo, y
 * abajo la grilla de tarjetas.
 */
@Composable
fun CvsContent(
    uiState: CvsUiState,
    savedCv: Cv?,
    onQueryChange: (String) -> Unit,
    onRetry: () -> Unit,
    onDeleteClick: (String) -> Unit,
    onDownloadCv: (Cv) -> Unit,
    onSaveCv: (Cv) -> Unit,
    onSaveHandled: () -> Unit,
    onTemplatesClick: () -> Unit,
    modifier: Modifier = Modifier,
    aiState: CvAiUiState = CvAiUiState(),
    aiActions: CvAiActions = CvAiActions(),
    onEditorChanged: () -> Unit = {},
    cvToOpen: Cv? = null,
    onCvOpened: () -> Unit = {},
    onAiCvCreated: () -> Unit = {},
) {
    // Modo IA abierto. Al crear el CV se recarga la lista, que lo abre en el editor.
    var showAiCv by remember { mutableStateOf(false) }
    if (showAiCv) {
        AiCvSheet(
            onDismiss = { showAiCv = false },
            onCreated = {
                showAiCv = false
                onAiCvCreated()
            },
        )
    }

    // Currículum abierto en el editor, o null si no hay ninguno.
    var editingCv by remember { mutableStateOf<Cv?>(null) }

    // CV recién creado desde Plantillas: se abre solo en el editor para empezar a llenarlo.
    LaunchedEffect(cvToOpen) {
        if (cvToOpen != null) {
            editingCv = cvToOpen
            onCvOpened()
        }
    }

    // Al abrir o cerrar el editor la IA vuelve a cero: una respuesta que llegue tarde no
    // tiene que caer en otro currículum.
    LaunchedEffect(editingCv?.id) { onEditorChanged() }
    // Currículum que el usuario pidió borrar, a la espera de que confirme.
    var cvToDelete by remember { mutableStateOf<Cv?>(null) }

    editingCv?.let { cv ->
        CvEditorSheet(
            cv = cv,
            isSaving = uiState.isSaving,
            onSave = onSaveCv,
            onDismiss = { editingCv = null },
            aiState = aiState,
            aiActions = aiActions,
        )
    }

    // Cuando el servicio confirma el guardado, se cierra el editor solo.
    LaunchedEffect(savedCv) {
        if (savedCv != null) {
            editingCv = null
            onSaveHandled()
        }
    }

    cvToDelete?.let { cv ->
        AlertDialog(
            onDismissRequest = { cvToDelete = null },
            title = { Text(stringResource(R.string.cvs_delete_title)) },
            text = { Text(stringResource(R.string.cvs_delete_message, cv.title)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteClick(cv.id)
                        cvToDelete = null
                    },
                ) {
                    Text(
                        text = stringResource(R.string.cvs_delete_confirm),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { cvToDelete = null }) {
                    Text(stringResource(R.string.cvs_delete_cancel))
                }
            },
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        CvsHeader(query = uiState.query, onQueryChange = onQueryChange)

        CreateCvBanner(
            onTemplatesClick = onTemplatesClick,
            onAiClick = { showAiCv = true },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )

        when {
            uiState.isLoading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            uiState.error != null -> CvsError(message = stringResource(uiState.error), onRetry = onRetry)

            uiState.cvs.isEmpty() -> EmptyState(
                title = stringResource(R.string.cvs_empty_title),
                message = stringResource(R.string.cvs_empty_message),
            )

            else -> LazyColumn(
                // Una sola columna, con la tarjeta a todo el ancho. En dos columnas la
                // vista previa quedaba tan angosta que no se leía nada, y los botones de
                // Ver/Editar y Descargar se apretaban. En la web son varias columnas
                // porque hay ancho de sobra; en el teléfono no.
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(items = uiState.cvs, key = { it.id }) { cv ->
                    CvCard(
                        cv = cv,
                        preview = uiState.previews[cv.id],
                        isDeleting = uiState.deletingId == cv.id,
                        onDeleteClick = { cvToDelete = cv },
                        onCvClick = { editingCv = it },
                        onDownloadClick = { onDownloadCv(cv) },
                    )
                }
            }
        }
    }
}

/** Antetítulo, título y buscador. */
@Composable
private fun CvsHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = {
            Text(stringResource(R.string.cvs_search_placeholder))
        },
        singleLine = true,
        shape = RoundedCornerShape(50),
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

/**
 * Recuadro para crear un currículum.
 *
 * Antes tenía dos botones, "IA" y "Offline", y el de IA solo abría un aviso que decía que había
 * que elegir una plantilla. Como la IA es lo esencial de la app, ahora el recuadro lo dice de
 * entrada (cómo se usa) y tiene una sola acción: elegir la plantilla. La IA se usa después,
 * en el editor, con "Mejorar con IA".
 */
@Composable
private fun CreateCvBanner(
    onTemplatesClick: () -> Unit,
    onAiClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(R.string.create_cv_ai_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            Text(
                text = stringResource(R.string.create_cv_ai_steps),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.padding(top = 6.dp),
            )
            // Dos caminos: que la IA arme todo desde lo que cuentas, o elegir una plantilla y
            // llenarla con ayuda de "Mejorar con IA" en el editor.
            Button(
                onClick = onAiClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            ) {
                Text(stringResource(R.string.create_cv_ai_write))
            }
            OutlinedButton(
                onClick = onTemplatesClick,
                colors = outlinedOnCardColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                Text(stringResource(R.string.create_cv_ai_action))
            }
        }
    }
}

/** Tarjeta de un currículum: vista previa, título, fecha y acciones. */
@Composable
private fun CvCard(
    cv: Cv,
    preview: Bitmap?,
    isDeleting: Boolean,
    onDeleteClick: () -> Unit,
    onCvClick: (Cv) -> Unit,
    onDownloadClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column {
            // El modelo del CV no trae imagen de vista previa: por ahora va el recuadro gris
            // del diseño.
            CvPreview(
                cv = cv,
                page = preview,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp),
            )

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = cv.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.home_cv_updated_at, cv.updatedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 10.dp),
                ) {
                    Button(
                        onClick = { onCvClick(cv) },
                        contentPadding = TIGHT_BUTTON_PADDING,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = stringResource(R.string.home_cv_edit),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                        )
                    }
                    Button(
                        onClick = onDownloadClick,
                        contentPadding = TIGHT_BUTTON_PADDING,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onSurface,
                            contentColor = MaterialTheme.colorScheme.surface,
                        ),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = stringResource(R.string.home_cv_download),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                        )
                    }
                }

                // El diseño no dibuja el borrado, pero es la única operación de eliminar del
                // CRUD y la rúbrica la pide, así que queda discreta al pie.
                TextButton(
                    onClick = onDeleteClick,
                    enabled = !isDeleting,
                    contentPadding = TIGHT_BUTTON_PADDING,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(
                        text = stringResource(
                            if (isDeleting) R.string.cvs_deleting else R.string.cvs_delete,
                        ),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun CvsError(
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
                text = stringResource(R.string.cvs_error_title),
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

/** Relleno angosto: los dos botones de la tarjeta tienen que caber en una columna estrecha. */
private val TIGHT_BUTTON_PADDING = PaddingValues(horizontal = 6.dp, vertical = 4.dp)

@Preview(name = "Currículums · claro", widthDp = 390, heightDp = 900)
@Composable
private fun CvsPreviewLight() = CvsPreview(darkTheme = false)

@Preview(name = "Currículums · oscuro", widthDp = 390, heightDp = 900)
@Composable
private fun CvsPreviewDark() = CvsPreview(darkTheme = true)

@Composable
private fun CvsPreview(darkTheme: Boolean) {
    CvProTheme(darkTheme = darkTheme) {
        Surface(color = MaterialTheme.colorScheme.background) {
            CvsContent(
                uiState = CvsUiState(),
                savedCv = null,
                onQueryChange = {},
                onRetry = {},
                onDeleteClick = {},
                onDownloadCv = {},
                onSaveCv = {},
                onSaveHandled = {},
                onTemplatesClick = {},
            )
        }
    }

}

