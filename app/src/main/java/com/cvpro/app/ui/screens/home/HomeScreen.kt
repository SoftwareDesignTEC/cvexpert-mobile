package com.cvpro.app.ui.screens.home

import android.content.Intent
import android.graphics.Bitmap
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cvpro.app.R
import com.cvpro.app.domain.model.Cv
import com.cvpro.app.domain.model.JobOffer
import com.cvpro.app.ui.screens.cvs.CvDocumentViewer
import com.cvpro.app.ui.components.BlockingProgressDialog
import com.cvpro.app.ui.components.CvCardActions
import com.cvpro.app.ui.components.PagePreview
import com.cvpro.app.ui.components.PreviewPlaceholder
import com.cvpro.app.ui.components.RemoteImage
import com.cvpro.app.ui.components.VersionLabel
import com.cvpro.app.ui.components.outlinedOnCardColors
import com.cvpro.app.ui.screens.cvs.AiCvSheet
import com.cvpro.app.ui.theme.CvProTheme
import com.cvpro.app.ui.theme.extraColors


/** Panel Principal: la pantalla de entrada después del login. */
@Composable
fun HomeScreen(
    versionName: String,
    versionCode: Int,
    onTemplatesClick: () -> Unit,
    onJobsClick: () -> Unit,
    onCvsClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val generatedPdf by viewModel.generatedPdf.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Cada vez que se entra al panel se recarga (ver onScreenShown).
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

    HomeContent(
        uiState = uiState,
        versionName = versionName,
        versionCode = versionCode,
        onTemplatesClick = onTemplatesClick,
        onJobsClick = onJobsClick,
        onCvsClick = onCvsClick,
        onDownloadCv = viewModel::onDownloadCv,
        onEditCv = {
            // Mis Currículums lo abre en el editor apenas se muestre.
            viewModel.onEditCv(it)
            onCvsClick()
        },
        modifier = modifier,
    )
}

/**
 * Panel Principal, con las cuatro secciones del diseño de Samuel: los currículums recientes,
 * la barra para crear uno, el análisis de habilidades y las ofertas recomendadas.
 *
 * Cada sección es una tarjeta blanca sobre el fondo azulado del tema.
 */
@Composable
fun HomeContent(
    uiState: HomeUiState,
    versionName: String,
    versionCode: Int,
    onTemplatesClick: () -> Unit,
    onJobsClick: () -> Unit,
    onCvsClick: () -> Unit,
    onDownloadCv: (Cv) -> Unit,
    modifier: Modifier = Modifier,
    onEditCv: (Cv) -> Unit = {},
) {
    var detailCv by remember { mutableStateOf<Cv?>(null) }
    // Modo IA abierto. Cuando crea el CV, se va a Mis Currículums, que lo abre en el editor.
    var showAiCv by remember { mutableStateOf(false) }
    if (showAiCv) {
        AiCvSheet(
            onDismiss = { showAiCv = false },
            onCreated = {
                showAiCv = false
                onCvsClick()
            },
        )
    }

    detailCv?.let { cv ->
        CvDocumentViewer(
            cv = cv,
            onDismiss = { detailCv = null },
            onEditClick = {
                detailCv = null
                onEditCv(cv)
            },
            onDownloadClick = { onDownloadCv(cv) },
        )
    }
    if (uiState.isGeneratingPdf) BlockingProgressDialog(stringResource(R.string.pdf_preparing))

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        RecentCvsSection(
            cvs = uiState.recentCvs,
            previews = uiState.cvPreviews,
            isLoading = uiState.isLoading,
            onSeeAllClick = onCvsClick,
            onCvClick = { detailCv = it },
            onEditClick = onEditCv,
            onDownloadClick = onDownloadCv,
        )

        CreateCvSection(onTemplatesClick = onTemplatesClick, onAiClick = { showAiCv = true })


        RecommendedOffersSection(
            offers = uiState.recommendedOffers,
            isLoading = uiState.isLoading,
            onSeeAllClick = onJobsClick,
        )

        VersionLabel(
            versionName = versionName,
            versionCode = versionCode,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        )
    }
}

// ─── Currículums recientes ────────────────────────────────────────────────────

@Composable
private fun RecentCvsSection(
    cvs: List<Cv>,
    previews: Map<String, Bitmap>,
    isLoading: Boolean,
    onSeeAllClick: () -> Unit,
    onCvClick: (Cv) -> Unit,
    onEditClick: (Cv) -> Unit,
    onDownloadClick: (Cv) -> Unit,
    modifier: Modifier = Modifier,
) {
    SectionCard(modifier = modifier) {
        SectionHeader(
            title = stringResource(R.string.home_recent_cvs_title),
            actionLabel = stringResource(R.string.home_recent_cvs_see_all),
            onActionClick = onSeeAllClick,
        )

        if (isLoading && cvs.isEmpty()) {
            SectionLoading(stringResource(R.string.loading_cvs))
        } else if (cvs.isEmpty()) {
            EmptySectionText(stringResource(R.string.home_recent_cvs_empty))
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 12.dp),
            ) {
                items(items = cvs, key = { it.id }) { cv ->
                    RecentCvCard(
                        cv = cv,
                        preview = previews[cv.id],
                        onCvClick = { onCvClick(cv) },
                        onEditClick = { onEditClick(cv) },
                        onDownloadClick = { onDownloadClick(cv) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentCvCard(
    cv: Cv,
    preview: Bitmap?,
    onCvClick: () -> Unit,
    onEditClick: () -> Unit,
    onDownloadClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onCvClick,
        modifier = modifier.width(250.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column {
            // Primero la hoja real del CV dibujada con su plantilla. Si todavía no está, la
            // imagen que manda el servicio y, si tampoco, el recuadro con el rótulo.
            // Mientras se dibuja la hoja, una rueda en vez de un recuadro vacío.
            if (preview != null) {
                PagePreview(
                    page = preview,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                )
            } else {
                PreviewPlaceholder(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                )
            }

            Column(modifier = Modifier.padding(start = 10.dp, end = 0.dp, top = 6.dp, bottom = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = cv.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = stringResource(R.string.home_cv_updated_at, cv.updatedAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    CvCardActions(onEditClick = onEditClick, onDownloadClick = onDownloadClick)
                }

                Button(
                    onClick = onCvClick,
                    contentPadding = TIGHT_BUTTON_PADDING,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, end = 10.dp),
                ) {
                    Text(
                        text = stringResource(R.string.cv_card_view),
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

// ─── Crear currículum ─────────────────────────────────────────────────────────

/**
 * Recuadro para crear un currículum.
 *
 * Antes tenía dos botones, "IA" y "Offline", y el de IA solo abría un aviso que decía que había
 * que elegir una plantilla. Como la IA es lo esencial de la app, ahora el recuadro lo dice de
 * entrada (cómo se usa) y tiene una sola acción: elegir la plantilla. La IA se usa después,
 * en el editor, con "Mejorar con IA".
 */
@Composable
private fun CreateCvSection(
    onTemplatesClick: () -> Unit,
    onAiClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
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
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            Text(
                text = stringResource(R.string.create_cv_ai_steps),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
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


@Composable
private fun RecommendedOffersSection(
    offers: List<JobOffer>,
    isLoading: Boolean,
    onSeeAllClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SectionCard(modifier = modifier) {
        SectionHeader(
            title = stringResource(R.string.home_offers_title),
            actionLabel = stringResource(R.string.home_offers_see_all),
            onActionClick = onSeeAllClick,
        )

        if (isLoading && offers.isEmpty()) {
            SectionLoading(stringResource(R.string.loading_jobs))
        } else if (offers.isEmpty()) {
            EmptySectionText(stringResource(R.string.home_offers_empty))
        } else {
            offers.forEach { offer ->
                OfferRow(offer = offer, modifier = Modifier.padding(top = 10.dp))
            }
        }
    }
}

@Composable
private fun OfferRow(
    offer: JobOffer,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(10.dp),
        ) {
            CompanyLogo(logoUrl = offer.companyLogoUrl, company = offer.company)

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(
                        R.string.home_offer_title_company,
                        offer.title,
                        offer.company,
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = offer.location,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // El porcentaje de compatibilidad va en una píldora verde, como en el diseño.
            offer.matchScore?.let { score ->
                Surface(
                    color = MaterialTheme.extraColors.successContainer,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.padding(start = 6.dp),
                ) {
                    Text(
                        text = stringResource(R.string.home_offer_match, score),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.extraColors.onSuccessContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}

// ─── Piezas compartidas ───────────────────────────────────────────────────────

/**
 * Título de sección con la barra azul a la izquierda, y opcionalmente un botón a la derecha.
 */
@Composable
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: () -> Unit = {},
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
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
        )
        if (actionLabel != null) {
            TextButton(
                onClick = onActionClick,
                contentPadding = PaddingValues(
                    horizontal = 6.dp,
                    vertical = 4.dp,
                ),
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    softWrap = false,
                )

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/** La sección todavía está cargando: rueda con su texto, para que no parezca vacía. */
@Composable
private fun SectionLoading(
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
    ) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

/** Mensaje de sección sin datos. */
@Composable
private fun EmptySectionText(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
    )
}

/** Tarjeta blanca que envuelve cada sección del panel. */
@Composable
private fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp), content = content)
    }
}

/** Recuadro del logo de la empresa, con su inicial cuando no hay imagen. */
@Composable
private fun CompanyLogo(
    logoUrl: String?,
    company: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        RemoteImage(
            url = logoUrl,
            contentDescription = stringResource(R.string.applications_logo_description, company),
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            fallback = {
                Text(
                    text = company.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            },
        )
    }
}

/** Relleno angosto: en el panel hay varios botones por fila y el de Material no cabe. */
private val TIGHT_BUTTON_PADDING = PaddingValues(horizontal = 10.dp, vertical = 4.dp)

@Preview(name = "Panel · claro", widthDp = 390, heightDp = 1200)
@Composable
private fun HomeScreenPreviewLight() = HomePreview(darkTheme = false)

@Preview(name = "Panel · oscuro", widthDp = 390, heightDp = 1200)
@Composable
private fun HomeScreenPreviewDark() = HomePreview(darkTheme = true)

/** Envuelve los previews con el fondo del tema, para que el modo oscuro no salga sobre blanco. */
@Composable
private fun HomePreview(darkTheme: Boolean) {
    CvProTheme(darkTheme = darkTheme) {
        Surface(color = MaterialTheme.colorScheme.background) {
            HomeContent(
                uiState = HomeUiState(),
                versionName = "0.3.0",
                versionCode = 3,
                onTemplatesClick = {},
                onJobsClick = {},
                onCvsClick = {},
                onDownloadCv = {},
            )
        }
    }
}
