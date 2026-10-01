package com.cvpro.app.ui.screens.cvs

import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import android.content.Intent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cvpro.app.R
import com.cvpro.app.domain.model.Cv
import androidx.annotation.StringRes
import com.cvpro.app.ui.components.ComingSoonSheet
import com.cvpro.app.ui.components.CvEditorSheet
import com.cvpro.app.ui.components.EmptyState
import com.cvpro.app.ui.theme.CvProTheme


@Composable
private fun CvPreview(
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
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val generatedPdf by viewModel.generatedPdf.collectAsStateWithLifecycle()
    val savedCv by viewModel.savedCv.collectAsStateWithLifecycle()
    val context = LocalContext.current

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
) {
    // Currículum abierto en el editor, o null si no hay ninguno.
    var editingCv by remember { mutableStateOf<Cv?>(null) }
    var comingSoon by remember { mutableStateOf<ComingSoon?>(null) }
    // Currículum que el usuario pidió borrar, a la espera de que confirme.
    var cvToDelete by remember { mutableStateOf<Cv?>(null) }

    editingCv?.let { cv ->
        CvEditorSheet(
            cv = cv,
            isSaving = uiState.isSaving,
            onSave = onSaveCv,
            onDismiss = { editingCv = null },
        )
    }

    // Cuando el servicio confirma el guardado, se cierra el editor solo.
    LaunchedEffect(savedCv) {
        if (savedCv != null) {
            editingCv = null
            onSaveHandled()
        }
    }
    comingSoon?.let { aviso ->
        ComingSoonSheet(
            title = stringResource(aviso.titleRes),
            message = stringResource(aviso.messageRes),
            onDismiss = { comingSoon = null },
        )
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
            onOfflineClick = onTemplatesClick,
            onAiClick = { comingSoon = ComingSoon.AI },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )

        when {
            uiState.isLoading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            uiState.error != null -> CvsError(message = uiState.error, onRetry = onRetry)

            uiState.cvs.isEmpty() -> EmptyState(
                title = stringResource(R.string.cvs_empty_title),
                message = stringResource(R.string.cvs_empty_message),
            )

            else -> LazyVerticalGrid(
                // Dos columnas: es lo que entra cómodo en un teléfono. En el diseño web son
                // tres porque hay más ancho.
                columns = GridCells.Adaptive(
                    minSize = 180.dp,
                ),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(items = uiState.cvs, key = { it.id }) { cv ->
                    CvCard(
                        cv = cv,
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

/** Banda oscura con los dos caminos para crear un currículum. */
@Composable
private fun CreateCvBanner(
    onOfflineClick: () -> Unit,
    onAiClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.home_create_cv_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.surface,
            )
            Text(
                text = stringResource(R.string.cvs_create_subtitle),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.surface,
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 12.dp),
            ) {
                Button(
                    onClick = onAiClick,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.cvs_create_ai))
                }
                Button(
                    onClick = onOfflineClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.cvs_create_offline))
                }
            }
        }
    }
}

/** Tarjeta de un currículum: vista previa, título, fecha y acciones. */
@Composable
private fun CvCard(
    cv: Cv,
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
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

/** Los avisos de funciones que llegan en la Fase II. */
private enum class ComingSoon(
    @param:StringRes val titleRes: Int,
    @param:StringRes val messageRes: Int,
) {
    AI(R.string.coming_soon_ai_title, R.string.coming_soon_ai_message),
}
