package com.cvpro.app.ui.screens.cvs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cvpro.app.domain.model.Cv
import com.cvpro.app.ui.components.EmptyState

@Composable
fun CvsScreen(
    modifier: Modifier = Modifier,
    viewModel: CvsViewModel = hiltViewModel(),
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
                        text = "No se pudieron cargar los currículums",
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

        uiState.cvs.isEmpty() -> {
            EmptyState(
                title = "No hay currículums",
                message = "Todavía no hay currículums disponibles.",
            )
        }

        else -> {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(
                    items = uiState.cvs,
                    key = { it.id },
                ) { cv ->
                    CvCard(
                        cv = cv,
                        isDeleting = uiState.deletingId == cv.id,
                        onDelete = { viewModel.deleteCv(cv.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CvCard(
    cv: Cv,
    isDeleting: Boolean,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = cv.title,
                style = MaterialTheme.typography.titleMedium,
            )

            Text(
                text = cv.personalInfo.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                text = "${cv.skills.size} habilidades",
                style = MaterialTheme.typography.bodySmall,
            )

            Text(
                text = "${cv.workExperiences.size} experiencias laborales",
                style = MaterialTheme.typography.bodySmall,
            )

            Text(
                text = "Actualizado: ${cv.updatedAt}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    onClick = onDelete,
                    enabled = !isDeleting,
                ) {
                    Text(
                        if (isDeleting) {
                            "Eliminando..."
                        } else {
                            "Eliminar"
                        }
                    )
                }
            }
        }
    }
}