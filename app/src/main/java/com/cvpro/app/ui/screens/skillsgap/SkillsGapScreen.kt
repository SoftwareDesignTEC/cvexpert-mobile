package com.cvpro.app.ui.screens.skillsgap

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cvpro.app.R
import com.cvpro.app.domain.model.CourseRecommendation
import com.cvpro.app.domain.model.SkillGapItem
import com.cvpro.app.domain.model.SkillsGapAnalysis
import com.cvpro.app.ui.components.ErrorState
import com.cvpro.app.ui.components.LoadingState
import com.cvpro.app.ui.components.RadarChart
import com.cvpro.app.ui.components.RadarLegend
import com.cvpro.app.ui.components.RadarSeries
import com.cvpro.app.ui.theme.CvProTheme

/** Análisis de Brechas con estado. */
@Composable
fun SkillsGapScreen(
    modifier: Modifier = Modifier,
    viewModel: SkillsGapViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SkillsGapContent(
        uiState = uiState,
        onRetryClick = viewModel::onRetryClick,
        modifier = modifier,
    )
}

/**
 * Compara tus habilidades con las que piden las ofertas del puesto al que apuntás.
 *
 * Arriba el radar con las dos capas, debajo las habilidades donde estás por debajo del
 * mercado, y al final los cursos que ayudan a cerrar esa diferencia.
 */
@Composable
fun SkillsGapContent(
    uiState: SkillsGapUiState,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val analysis = uiState.analysis

    when {
        uiState.isLoading -> LoadingState(modifier = modifier)

        uiState.errorMessage != null -> ErrorState(
            message = stringResource(uiState.errorMessage),
            onRetryClick = onRetryClick,
            modifier = modifier,
        )

        analysis != null -> SkillsGapList(analysis = analysis, modifier = modifier)
    }
}

@Composable
private fun SkillsGapList(analysis: SkillsGapAnalysis, modifier: Modifier = Modifier) {
    val userColor = MaterialTheme.colorScheme.primary
    val marketColor = MaterialTheme.colorScheme.tertiary
    val series = listOf(
        RadarSeries(stringResource(R.string.skills_gap_series_user), analysis.skills.map { it.userLevel }, userColor),
        RadarSeries(stringResource(R.string.skills_gap_series_market), analysis.skills.map { it.marketLevel }, marketColor),
    )

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxSize(),
    ) {
        item {
            Text(
                text = stringResource(R.string.skills_gap_target, analysis.targetJobTitle),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    RadarChart(
                        labels = analysis.skills.map { it.name },
                        series = series,
                    )
                    RadarLegend(series = series, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }

        item {
            Text(
                text = stringResource(R.string.skills_gap_to_improve_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        if (analysis.skillsToImprove.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.skills_gap_no_gaps),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(items = analysis.skillsToImprove, key = { it.name }) { skill ->
                SkillGapRow(skill = skill)
            }
        }

        item {
            Text(
                text = stringResource(R.string.skills_gap_courses_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        items(items = analysis.recommendations, key = { it.id }) { course ->
            CourseCard(course = course)
        }
    }
}

/** Una habilidad por mejorar, con cuánto le falta para llegar al nivel del mercado. */
@Composable
private fun SkillGapRow(skill: SkillGapItem, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = skill.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.skills_gap_levels, skill.userLevel, skill.marketLevel),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = stringResource(R.string.skills_gap_amount, skill.gap),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

/** Curso recomendado para cerrar una brecha. */
@Composable
private fun CourseCard(course: CourseRecommendation, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = course.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = course.platform,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(
                    R.string.skills_gap_course_details,
                    course.duration ?: stringResource(R.string.skills_gap_course_no_duration),
                    stringResource(if (course.isPaid) R.string.skills_gap_course_paid else R.string.skills_gap_course_free),
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Preview(name = "Brechas - claro")
@Composable
private fun SkillsGapPreview() {
    CvProTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            SkillsGapContent(
                uiState = SkillsGapUiState(
                    analysis = SkillsGapAnalysis(
                        targetJobTitle = "Asistente administrativo",
                        skills = listOf(
                            SkillGapItem("Comunicación", 70, 80),
                            SkillGapItem("Excel", 45, 85),
                            SkillGapItem("Inglés", 40, 75),
                            SkillGapItem("Organización", 60, 80),
                            SkillGapItem("Trabajo en equipo", 75, 65),
                        ),
                        recommendations = listOf(
                            CourseRecommendation("1", "Excel desde cero", "YouTube", "https://example.com", false, "6 horas"),
                        ),
                    ),
                ),
                onRetryClick = {},
            )
        }
    }
}
