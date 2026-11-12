package com.cvpro.app.ui.screens.jobs

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.cvpro.app.ui.components.JobOfferDetailSheet
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cvpro.app.ui.components.ApplyButton
import com.cvpro.app.ui.components.ApplyState
import com.cvpro.app.ui.components.LocalSnackbarHostState
import com.cvpro.app.ui.components.RemoteImage
import com.cvpro.app.ui.components.outlinedOnCardColors
import com.cvpro.app.R
import com.cvpro.app.domain.model.JobModality
import com.cvpro.app.domain.model.JobOffer
import com.cvpro.app.ui.screens.skillsgap.JobSkillsGapSheet
import com.cvpro.app.ui.components.CompatibilityRing
import com.cvpro.app.ui.components.EmptyState
import com.cvpro.app.ui.theme.CvProTheme

/** Ofertas Laborales con estado. */
@Composable
fun JobsScreen(
    modifier: Modifier = Modifier,
    viewModel: JobsViewModel = hiltViewModel(),
    onCvsClick: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // El Scaffold de la raíz comparte un único snackbar, igual que en Plantillas.
    val snackbarHostState = LocalSnackbarHostState.current
    val resources = LocalResources.current

    // Después de registrar la postulación a una oferta real, se abre su página para terminarla.
    val uriHandler = LocalUriHandler.current
    LaunchedEffect(uiState.urlToOpen) {
        val url = uiState.urlToOpen ?: return@LaunchedEffect
        runCatching { uriHandler.openUri(url) }
        viewModel.onUrlOpened()
    }

    // Cada vez que se entra se revisa a qué ofertas ya se postuló el usuario.
    LaunchedEffect(Unit) { viewModel.onScreenShown() }

    // El resultado de postularse se avisa abajo y se limpia solo.
    LaunchedEffect(uiState.message) {
        val mensaje = uiState.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(resources.getString(mensaje.res, *mensaje.args.toTypedArray()))
        viewModel.onMessageShown()
    }

    JobsContent(
        uiState = uiState,
        onQueryChange = viewModel::onQueryChange,
        onFilterChange = viewModel::onFilterChange,
        onRetryClick = viewModel::onRetryClick,
        onLoadMore = viewModel::onLoadMore,
        onApply = viewModel::onApply,
        modifier = modifier,
        onCvsClick = onCvsClick,
    )
}

/**
 * Ofertas Laborales, siguiendo el diseño de Samuel.
 *
 * Arriba el encabezado con el contador y el buscador, después los filtros rápidos, y abajo
 * la lista de ofertas que se va cargando por páginas al llegar al final.
 */
@Composable
fun JobsContent(
    uiState: JobsUiState,
    onQueryChange: (String) -> Unit,
    onFilterChange: (JobsFilter) -> Unit,
    onRetryClick: () -> Unit,
    onLoadMore: () -> Unit,
    onApply: (JobOffer) -> Unit,
    modifier: Modifier = Modifier,
    onCvsClick: () -> Unit = {},
) {
    // Oferta abierta en el detalle, o null si la hoja está cerrada.
    var detailOffer by remember { mutableStateOf<JobOffer?>(null) }
    var gapOffer by remember { mutableStateOf<JobOffer?>(null) }

    gapOffer?.let { offer ->
        JobSkillsGapSheet(offer = offer, onDismiss = { gapOffer = null }, onCorrectCv = { gapOffer = null; onCvsClick() })
    }

    detailOffer?.let { offer ->
        JobOfferDetailSheet(
            offer = offer,
            onDismiss = { detailOffer = null },
            applyState = uiState.applyStateOf(offer),
            onApply = { onApply(offer) },
            onAnalyzeGap = { detailOffer = null; gapOffer = offer },
        )
    }

    // Cuántas ofertas se muestran: al principio solo INITIAL_OFFERS. Vuelve a empezar si cambia
    // el filtro o la búsqueda.
    var showAll by remember(uiState.selectedFilter, uiState.query) { mutableStateOf(false) }
    val shownOffers = if (showAll) uiState.visibleOffers else uiState.visibleOffers.take(INITIAL_OFFERS)

    Column(modifier = modifier.fillMaxSize()) {
        JobsHeader(
            query = uiState.query,
            totalRecords = uiState.totalRecords,
            onQueryChange = onQueryChange,
        )

        JobsFilters(
            selected = uiState.selectedFilter,
            onFilterChange = onFilterChange,
        )

        when {
            uiState.isLoading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            uiState.errorMessage != null -> JobsError(
                messageRes = uiState.errorMessage,
                onRetryClick = onRetryClick,
            )

            uiState.isEmpty -> EmptyState(
                title = stringResource(R.string.jobs_empty_title),
                // Si hay ofertas pero ninguna pasa el filtro rápido, el problema no es la
                // búsqueda: se dice que pruebe con otro filtro.
                message = stringResource(
                    if (uiState.offers.isNotEmpty() && uiState.selectedFilter != JobsFilter.RECOMMENDED) {
                        R.string.jobs_empty_filter_message
                    } else {
                        R.string.jobs_empty_message
                    },
                ),
            )

            else -> LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(items = shownOffers, key = { it.id }) { offer ->
                    JobCard(
                        offer = offer,
                        applyState = uiState.applyStateOf(offer),
                        onDetailsClick = { detailOffer = offer },
                        onApplyClick = { onApply(offer) },
                    )
                }

                // Primero se ven unas pocas; el resto, a pedido. Con todas de una la lista
                // quedaba muy larga ("chorreada", según la retroalimentación del equipo).
                val hidden = uiState.visibleOffers.size - shownOffers.size
                if (hidden > 0) {
                    item {
                        OutlinedButton(
                            onClick = { showAll = true },
                            colors = outlinedOnCardColors(),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.jobs_show_more, hidden))
                        }
                    }
                }

                if (showAll && uiState.hasMore) {
                    item {
                        // Antes la página siguiente se pedía sola al llegar al pie y la
                        // lista no terminaba nunca. Ahora la pide el usuario: ve cuántas
                        // le faltan y decide si quiere seguir.
                        LoadMoreButton(
                            isLoading = uiState.isLoadingMore,
                            remaining = (uiState.totalRecords - uiState.offers.size)
                                .coerceAtLeast(0),
                            onClick = onLoadMore,
                        )
                    }
                }
            }
        }
    }
}

/** Antetítulo, título, contador de resultados y buscador. */
@Composable
private fun JobsHeader(
    query: String,
    totalRecords: Int,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(50),
            ) {
                Text(
                    text = stringResource(R.string.jobs_found_count, totalRecords),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text(stringResource(R.string.jobs_search_placeholder)) },
            singleLine = true,
            shape = RoundedCornerShape(50),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        )
    }
}

/** Fila de filtros rápidos. */
@Composable
private fun JobsFilters(
    selected: JobsFilter,
    onFilterChange: (JobsFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Con desplazamiento horizontal: los cuatro filtros no entran en el ancho de un
    // teléfono, y sin esto el último parte su texto en dos líneas.
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        JobsFilter.entries.forEach { filter ->
            FilterChip(
                selected = filter == selected,
                onClick = { onFilterChange(filter) },
                label = {
                    Text(
                        text = stringResource(filter.labelRes),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        softWrap = false,
                    )
                },
            )
        }
    }
}

/** Estado del botón "Postularme" de una oferta. */
private fun JobsUiState.applyStateOf(offer: JobOffer): ApplyState = when (offer.id) {
    applyingOfferId -> ApplyState.SENDING
    in appliedOfferIds -> ApplyState.APPLIED
    else -> ApplyState.AVAILABLE
}

/**
 * Tarjeta de una oferta: el logo y los datos arriba, el anillo de compatibilidad, los
 * requisitos, y abajo los botones de detalle y de postularse.
 */
@Composable
private fun JobCard(
    offer: JobOffer,
    applyState: ApplyState,
    onDetailsClick: () -> Unit,
    onApplyClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CompanyLogo(logoUrl = offer.companyLogoUrl, company = offer.company)

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = offer.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = offer.company,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(
                            R.string.jobs_location_and_modality,
                            offer.location,
                            stringResource(offer.modality.labelRes()),
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                offer.matchScore?.let { score ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CompatibilityRing(percentage = score)
                        Text(
                            text = stringResource(R.string.jobs_match_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Las ofertas reales casi nunca traen requisitos aparte, pero sí las habilidades que
            // el servicio saca del texto: si no hay requisitos, se muestran esas.
            val (summaryTitle, summary) = when {
                offer.requirements.isNotEmpty() ->
                    R.string.jobs_requirements_title to offer.requirements.joinToString(", ")
                offer.skills.isNotEmpty() ->
                    R.string.jobs_skills_title to offer.skills.joinToString(", ")
                else -> null to ""
            }
            if (summaryTitle != null) {
                Text(
                    text = stringResource(summaryTitle),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 12.dp),
                )
                // Dos renglones como mucho: los requisitos reales pueden ser oraciones largas.
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }

            // Detalle y postularse lado a lado. Postularse es la acción principal, por eso
            // va con el botón relleno y el detalle con el delineado.
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            ) {
                OutlinedButton(
                    onClick = onDetailsClick,
                    colors = outlinedOnCardColors(),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.jobs_details), maxLines = 1)
                }
                ApplyButton(
                    state = applyState,
                    onClick = onApplyClick,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun JobsError(
    messageRes: Int,
    onRetryClick: () -> Unit,
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
                text = stringResource(messageRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onRetryClick) {
                Text(stringResource(R.string.applications_retry))
            }
        }
    }
}

/**
 * Pie de la lista: pide la página siguiente cuando el usuario lo toca.
 *
 * Dice cuántas ofertas faltan, así se sabe si vale la pena seguir antes de tocarlo.
 */
@Composable
private fun LoadMoreButton(
    isLoading: Boolean,
    remaining: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = !isLoading,
        colors = outlinedOnCardColors(),
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = if (isLoading) {
                stringResource(R.string.jobs_loading_more)
            } else {
                stringResource(R.string.jobs_load_more, remaining)
            },
            style = MaterialTheme.typography.labelMedium,
        )
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
            .size(48.dp)
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

private fun JobModality.labelRes(): Int = when (this) {
    JobModality.REMOTE -> R.string.modality_remote
    JobModality.ON_SITE -> R.string.modality_on_site
    JobModality.HYBRID -> R.string.modality_hybrid
}

@Preview(name = "Ofertas · claro", widthDp = 390, heightDp = 900)
@Composable
private fun JobsPreviewLight() = JobsPreview(darkTheme = false)

@Preview(name = "Ofertas · oscuro", widthDp = 390, heightDp = 900)
@Composable
private fun JobsPreviewDark() = JobsPreview(darkTheme = true)

@Composable
private fun JobsPreview(darkTheme: Boolean) {
    CvProTheme(darkTheme = darkTheme) {
        Surface(color = MaterialTheme.colorScheme.background) {
            JobsContent(
                uiState = JobsUiState(
                    totalRecords = 2,
                    offers = listOf(
                        JobOffer(
                            id = "1",
                            title = "Data Analyst",
                            company = "TechCorp",
                            companyLogoUrl = null,
                            location = "Escazú, San José",
                            modality = JobModality.HYBRID,
                            sector = "Tecnología",
                            type = "full-time",
                            description = "Análisis de datos para decisiones de negocio.",
                            requirements = listOf("SQL avanzado", "Power BI"),
                            salary = null,
                            matchScore = 80,
                            postedAt = "13/09/2026",
                        ),
                        JobOffer(
                            id = "2",
                            title = "BI Developer",
                            company = "Nimbus",
                            companyLogoUrl = null,
                            location = "Heredia",
                            modality = JobModality.REMOTE,
                            sector = "Tecnología",
                            type = "full-time",
                            description = "Modelado de datos y tableros para el negocio.",
                            requirements = listOf("Modelado de datos", "ETL"),
                            salary = null,
                            matchScore = 76,
                            postedAt = "12/09/2026",
                        ),
                    ),
                ),
                onQueryChange = {},
                onFilterChange = {},
                onRetryClick = {},
                onLoadMore = {},
                onApply = {},
            )
        }
    }
}

/** Ofertas que se ven antes de tocar "Ver más ofertas". */
private const val INITIAL_OFFERS = 5
