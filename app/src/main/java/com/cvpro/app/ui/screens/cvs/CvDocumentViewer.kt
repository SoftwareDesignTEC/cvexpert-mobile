package com.cvpro.app.ui.screens.cvs

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cvpro.app.R
import com.cvpro.app.domain.model.Cv
import com.cvpro.app.ui.components.ErrorState
import com.cvpro.app.ui.components.LoadingState

/** Cuánto se acerca la hoja con dos toques. */
private const val ZOOM = 2f

/**
 * "Ver currículum": el CV tal como queda en el PDF descargado, hoja por hoja, a pantalla
 * completa. Antes era una lista de datos; ahora se ve el documento real con su plantilla.
 *
 * Arriba: la flecha para volver, el lápiz para editarlo y el botón para descargarlo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CvDocumentViewer(
    cv: Cv,
    onDismiss: () -> Unit,
    onEditClick: () -> Unit,
    onDownloadClick: () -> Unit,
    viewModel: CvViewerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(cv) { viewModel.show(cv) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            topBar = {
                TopAppBar(
                    title = { Text(cv.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cv_editor_back))
                        }
                    },
                    actions = {
                        IconButton(onClick = onEditClick) {
                            Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.cv_card_edit))
                        }
                        IconButton(onClick = onDownloadClick) {
                            Icon(Icons.Outlined.Download, contentDescription = stringResource(R.string.home_cv_download))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                )
            },
        ) { padding ->
            when {
                uiState.isLoading -> LoadingState(
                    modifier = Modifier.padding(padding),
                    message = stringResource(R.string.cv_viewer_loading),
                )
                uiState.failed -> ErrorState(
                    message = stringResource(R.string.cv_viewer_error_message),
                    onRetryClick = { viewModel.show(cv) },
                    modifier = Modifier.padding(padding),
                )
                else -> BoxWithConstraints(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                ) {
                    // Dos toques sobre la hoja la acercan al doble (para leer la letra chica) y
                    // se mueve de lado; dos toques más la devuelven a todo el ancho.
                    var zoomed by remember { mutableStateOf(false) }
                    val pageWidth = (maxWidth - 32.dp) * (if (zoomed) ZOOM else 1f)
                    Column {
                    // La ayuda queda fija arriba: no se mueve de lado con la hoja acercada.
                    Text(
                        text = stringResource(if (zoomed) R.string.cv_viewer_zoom_out else R.string.cv_viewer_zoom_in),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
                    )
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .then(if (zoomed) Modifier.horizontalScroll(rememberScrollState()) else Modifier),
                    ) {
                        itemsIndexed(uiState.pages) { index, page ->
                            // Cada hoja como una hoja de papel: blanca, con sombra, proporción A4.
                            Surface(shape = RoundedCornerShape(4.dp), shadowElevation = 4.dp) {
                                Image(
                                    bitmap = page.asImageBitmap(),
                                    contentDescription = stringResource(R.string.cv_viewer_page, index + 1, uiState.pages.size),
                                    contentScale = ContentScale.FillWidth,
                                    modifier = Modifier
                                        .width(pageWidth)
                                        .aspectRatio(page.width / page.height.toFloat())
                                        .background(MaterialTheme.colorScheme.surface)
                                        .pointerInput(Unit) { detectTapGestures(onDoubleTap = { zoomed = !zoomed }) },
                                )
                            }
                        }
                    }
                    }
                }
            }
        }
    }
}
