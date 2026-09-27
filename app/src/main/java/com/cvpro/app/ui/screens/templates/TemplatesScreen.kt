package com.cvpro.app.ui.screens.templates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cvpro.app.domain.model.CvTemplate
import com.cvpro.app.ui.components.EmptyState
import androidx.compose.foundation.layout.height
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage

@Composable
fun TemplatesScreen(
    modifier: Modifier = Modifier,
    viewModel: TemplatesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val error = uiState.error

    when {
        uiState.isLoading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }

        error != null -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "No se pudieron cargar las plantillas",
                        style = MaterialTheme.typography.titleMedium,
                    )

                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Button(onClick = viewModel::retry) {
                        Text("Reintentar")
                    }
                }
            }
        }

        uiState.templates.isEmpty() -> {
            EmptyState(
                title = "No hay plantillas",
                message = "Todavía no hay plantillas disponibles.",
            )
        }

        else -> {
            TemplatesList(
                templates = uiState.templates,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun TemplatesList(
    templates: List<CvTemplate>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(
            items = templates,
            key = { it.id },
        ) { template ->
            TemplateCard(template)
        }
    }
}

@Composable
private fun TemplateCard(
    template: CvTemplate,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AsyncImage(
                model = template.previewUrl,
                contentDescription = "Vista previa de ${template.name}",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentScale = ContentScale.Crop,
            )

            Text(
                text = template.name,
                style = MaterialTheme.typography.titleMedium,
            )

            Text(
                text = template.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                text = "Categoría: ${template.category}",
                style = MaterialTheme.typography.labelMedium,
            )

            Text(
                text = if (template.isPremium) {
                    "Plantilla premium"
                } else {
                    "Plantilla gratuita"
                },
                style = MaterialTheme.typography.labelMedium,
            )

            Text(
                text = if (template.availableOffline) {
                    "Disponible sin conexión"
                } else {
                    "Requiere conexión"
                },
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}