package com.cvpro.app.ui.screens.jobs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cvpro.app.R
import com.cvpro.app.domain.model.JobModality
import com.cvpro.app.domain.model.JobOffer
import com.cvpro.app.ui.components.EmptyState
import com.cvpro.app.ui.components.ErrorState
import com.cvpro.app.ui.components.FormTextField
import com.cvpro.app.ui.components.LoadingState
import com.cvpro.app.ui.theme.CvProTheme

/** Ofertas Laborales con estado: conecta el ViewModel con la pantalla. */
@Composable
fun JobsScreen(
    modifier: Modifier = Modifier,
    viewModel: JobsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    JobsContent(
        uiState = uiState,
        onQueryChange = viewModel::onQueryChange,
        onRetryClick = viewModel::onRetryClick,
        onLoadMore = viewModel::onLoadMore,
        modifier = modifier,
    )
}

/**
 * Lista de ofertas con buscador.
 *
 * Maneja los cuatro estados: cargando, error con reintentar, vacío y la lista. La
 * paginación es "scroll infinito": cuando el usuario se acerca al final, se pide la
 * página siguiente.
 */
@Composable
fun JobsContent(
    uiState: JobsUiState,
    onQueryChange: (String) -> Unit,
    onRetryClick: () -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        FormTextField(
            value = uiState.query,
            onValueChange = onQueryChange,
            label = stringResource(R.string.jobs_search_label),
            imeAction = ImeAction.Search,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        when {
            uiState.isLoading -> LoadingState()

            uiState.errorMessage != null -> ErrorState(
                message = stringResource(uiState.errorMessage),
                onRetryClick = onRetryClick,
            )

            uiState.isEmpty -> EmptyState(
                title = stringResource(R.string.jobs_empty_title),
                message = stringResource(R.string.jobs_empty_message),
            )

            else -> JobOfferList(
                offers = uiState.offers,
                totalRecords = uiState.totalRecords,
                isLoadingMore = uiState.isLoadingMore,
                hasMore = uiState.hasMore,
                onLoadMore = onLoadMore,
            )
        }
    }
}

@Composable
private fun JobOfferList(
    offers: List<JobOffer>,
    totalRecords: Int,
    isLoadingMore: Boolean,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    // Avisa cuando faltan pocas tarjetas para el final, así la página siguiente se pide
    // antes de que el usuario llegue al borde y la lista no se corta.
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= offers.size - PRELOAD_ITEMS
        }
    }
    LaunchedEffect(listState) {
        snapshotFlow { shouldLoadMore }.collect { cerca -> if (cerca) onLoadMore() }
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxSize(),
    ) {
        item {
            Text(
                text = pluralStringResource(R.plurals.jobs_result_count, totalRecords, totalRecords),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        items(items = offers, key = { it.id }) { offer ->
            JobOfferCard(offer = offer)
        }

        if (isLoadingMore) {
            item {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                ) {
                    CircularProgressIndicator()
                }
            }
        } else if (!hasMore && offers.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.jobs_end_of_list),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                )
            }
        }
    }
}

/** Tarjeta de una oferta: puesto, empresa, dónde y qué tanto encaja con el perfil. */
@Composable
private fun JobOfferCard(offer: JobOffer, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = offer.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = offer.company,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (offer.matchScore != null) {
                    Text(
                        text = stringResource(R.string.jobs_match_score, offer.matchScore),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Text(
                text = stringResource(
                    R.string.jobs_location_and_modality,
                    offer.location,
                    stringResource(offer.modality.labelRes()),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )

            offer.requirements.take(MAX_REQUIREMENTS).forEach { requisito ->
                Text(
                    text = stringResource(R.string.jobs_requirement_bullet, requisito),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

private fun JobModality.labelRes(): Int = when (this) {
    JobModality.REMOTE -> R.string.modality_remote
    JobModality.ON_SITE -> R.string.modality_on_site
    JobModality.HYBRID -> R.string.modality_hybrid
}

private const val PRELOAD_ITEMS = 3
private const val MAX_REQUIREMENTS = 2

@Preview(name = "Ofertas - lista")
@Composable
private fun JobsContentPreview() {
    CvProTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            JobsContent(
                uiState = JobsUiState(
                    offers = listOf(
                        JobOffer("1", "Cajero de tienda", "Automercado", null, "Heredia", JobModality.ON_SITE, "Comercio", listOf("Atención al cliente"), 76),
                        JobOffer("2", "Community manager junior", "Agencia Nova", null, "Remoto", JobModality.REMOTE, "Mercadeo", listOf("Redacción"), 58),
                    ),
                    totalRecords = 2,
                ),
                onQueryChange = {},
                onRetryClick = {},
                onLoadMore = {},
            )
        }
    }
}

@Preview(name = "Ofertas - vacío")
@Composable
private fun JobsEmptyPreview() {
    CvProTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            JobsContent(
                uiState = JobsUiState(query = "arquitecto naval"),
                onQueryChange = {},
                onRetryClick = {},
                onLoadMore = {},
            )
        }
    }
}
