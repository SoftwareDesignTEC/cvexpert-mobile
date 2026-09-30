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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.cvpro.app.ui.components.JobOfferDetailSheet
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
import com.cvpro.app.domain.model.JobModality
import com.cvpro.app.domain.model.JobOffer
import com.cvpro.app.ui.components.CompatibilityRing
import com.cvpro.app.ui.components.EmptyState
import com.cvpro.app.ui.theme.CvProTheme

/** Ofertas Laborales con estado. */
@Composable
fun JobsScreen(
    modifier: Modifier = Modifier,
    viewModel: JobsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    JobsContent(
        uiState = uiState,
        onQueryChange = viewModel::onQueryChange,
        onFilterChange = viewModel::onFilterChange,
        onRetryClick = viewModel::onRetryClick,
        onLoadMore = viewModel::onLoadMore,
        modifier = modifier,
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
    modifier: Modifier = Modifier,
) {
    // Oferta abierta en el detalle, o null si la hoja está cerrada.
    var detailOffer by remember { mutableStateOf<JobOffer?>(null) }

    detailOffer?.let { offer ->
        JobOfferDetailSheet(offer = offer, onDismiss = { detailOffer = null })
    }

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
                message = stringResource(R.string.jobs_empty_message),
            )

            else -> LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(items = uiState.visibleOffers, key = { it.id }) { offer ->
                    JobCard(offer = offer, onDetailsClick = { detailOffer = offer })
                }

                if (uiState.hasMore) {
                    item {
                        // Al llegar acá se pide la página siguiente: es el scroll infinito.
                        LaunchLoadMore(onLoadMore = onLoadMore)
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
        Text(
            text = stringResource(R.string.jobs_eyebrow),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.destination_jobs),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
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

/**
 * Tarjeta de una oferta: el logo y los datos arriba, el anillo de compatibilidad, los
 * requisitos, y el botón de detalle abajo.
 */
@Composable
private fun JobCard(
    offer: JobOffer,
    onDetailsClick: () -> Unit,
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

            if (offer.requirements.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.jobs_requirements_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 12.dp),
                )
                Text(
                    text = offer.requirements.joinToString(", "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }

            Button(
                onClick = onDetailsClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            ) {
                Text(stringResource(R.string.jobs_details))
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

/** Pie de la lista: muestra el cargando de la página siguiente y la dispara. */
@Composable
private fun LaunchLoadMore(
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.runtime.LaunchedEffect(Unit) { onLoadMore() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
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
        if (logoUrl != null) {
            AsyncImage(
                model = logoUrl,
                contentDescription = stringResource(R.string.applications_logo_description, company),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
            )
        } else {
            Text(
                text = company.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
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
            )
        }
    }
}
