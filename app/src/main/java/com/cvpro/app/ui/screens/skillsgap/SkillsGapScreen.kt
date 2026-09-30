package com.cvpro.app.ui.screens.skillsgap

import android.content.Intent
import androidx.core.net.toUri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cvpro.app.R
import com.cvpro.app.domain.model.CourseRecommendation
import com.cvpro.app.domain.model.SkillGapItem
import com.cvpro.app.domain.model.SkillsGapAnalysis
import com.cvpro.app.ui.components.RadarChart
import com.cvpro.app.ui.components.RadarSeries
import com.cvpro.app.ui.theme.CvProTheme

/** Análisis de Brecha con estado. */
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
 * Análisis de brecha, siguiendo el diseño de Samuel.
 *
 * Tres bloques: la comparativa contra el mercado con el radar, los puntos a mejorar
 * ordenados por brecha, y los cursos recomendados.
 */
@Composable
fun SkillsGapContent(
    uiState: SkillsGapUiState,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val analysis = uiState.analysis

    when {
        uiState.isLoading -> Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }

        uiState.errorMessage != null -> Box(
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
                    text = stringResource(uiState.errorMessage),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = onRetryClick) {
                    Text(stringResource(R.string.applications_retry))
                }
            }
        }

        analysis != null -> SkillsGapSections(analysis = analysis, modifier = modifier)
    }
}

@Composable
private fun SkillsGapSections(
    analysis: SkillsGapAnalysis,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SkillsGapHeader()
        ComparisonCard(analysis = analysis)
        ImprovementPointsCard(skills = analysis.skillsToImprove)
        CoursesCard(courses = analysis.recommendations)
        Spacer(modifier = Modifier.height(8.dp))
    }
}

/** Antetítulo, título y la marca de actualización. */
/** Antetítulo, título y la marca de actualización. */
@Composable
private fun SkillsGapHeader(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.skills_gap_eyebrow),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )

            Text(
                text = stringResource(R.string.skills_gap_screen_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        // TODO: el análisis no trae createdAt en el dominio, así que la fecha es fija.
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(50),
        ) {
            Text(
                text = stringResource(R.string.skills_gap_updated),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(
                    horizontal = 10.dp,
                    vertical = 5.dp,
                ),
            )
        }
    }
}


/** El radar con las dos series y su leyenda. */
@Composable
private fun ComparisonCard(
    analysis: SkillsGapAnalysis,
    modifier: Modifier = Modifier,
) {
    val userColor = MaterialTheme.colorScheme.primary
    val marketColor = MaterialTheme.colorScheme.tertiary

    SectionCard(modifier = modifier) {
        Text(
            text = stringResource(R.string.skills_gap_comparison_eyebrow),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.skills_gap_comparison_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        RadarChart(
            labels = analysis.skills.map { it.name },
            series = listOf(
                RadarSeries(
                    label = stringResource(R.string.skills_gap_series_user),
                    values = analysis.skills.map { it.userLevel },
                    color = userColor,
                ),
                RadarSeries(
                    label = stringResource(R.string.skills_gap_series_market),
                    values = analysis.skills.map { it.marketLevel },
                    color = marketColor,
                ),
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .padding(top = 12.dp),
        )

        // La leyenda va debajo y no al lado: en un teléfono, radar y leyenda lado a lado
        // dejan el gráfico demasiado chico.
        Column(modifier = Modifier.padding(top = 8.dp)) {
            LegendRow(
                color = userColor,
                label = stringResource(R.string.skills_gap_series_user),
            )
            LegendRow(
                color = marketColor,
                label = stringResource(R.string.skills_gap_series_market),
            )
        }
    }
}

@Composable
private fun LegendRow(
    color: Color,
    label: String,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(vertical = 3.dp),
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

/** Habilidades con más brecha, numeradas de mayor a menor. */
@Composable
private fun ImprovementPointsCard(
    skills: List<SkillGapItem>,
    modifier: Modifier = Modifier,
) {
    SectionCard(modifier = modifier) {
        Text(
            text = stringResource(R.string.skills_gap_points_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.skills_gap_points_subtitle),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (skills.isEmpty()) {
            Text(
                text = stringResource(R.string.skills_gap_no_gaps),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
        } else {
            skills.forEachIndexed { index, skill ->
                ImprovementRow(position = index + 1, skill = skill)
            }
        }
    }
}

@Composable
private fun ImprovementRow(
    position: Int,
    skill: SkillGapItem,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Barrita azul del diseño, pegada al borde izquierdo de la fila.
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .background(MaterialTheme.colorScheme.primary),
            )
            Text(
                text = stringResource(R.string.skills_gap_row_number, position),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 10.dp),
            )
            Text(
                text = skill.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
            )
            Text(
                text = stringResource(R.string.skills_gap_gap_percent, skill.gap),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 12.dp),
            )
        }
    }
}

/** Cursos sugeridos para cerrar las brechas. */
@Composable
private fun CoursesCard(
    courses: List<CourseRecommendation>,
    modifier: Modifier = Modifier,
) {
    SectionCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.skills_gap_courses_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            // La tarjeta ya lista todos los cursos que devuelve el análisis, así que en vez
            // del "Ver más" del diseño va el contador: no hay más que abrir.
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(50),
            ) {
                Text(
                    text = stringResource(R.string.skills_gap_courses_count, courses.size),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }
        }

        courses.forEach { course ->
            CourseCard(course = course, modifier = Modifier.padding(top = 10.dp))
        }
    }
}

@Composable
private fun CourseCard(
    course: CourseRecommendation,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Cuadrito con la inicial de la plataforma, como los logos del diseño.
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = course.platform.take(1).uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    text = course.platform,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 10.dp),
                )
            }

            Text(
                text = course.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 10.dp),
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 6.dp),
            ) {
                Text(
                    text = stringResource(
                        R.string.skills_gap_course_duration,
                        course.duration ?: stringResource(R.string.skills_gap_course_no_duration),
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Button(
                onClick = {
                    // Abre el curso en el navegador del teléfono.
                    context.startActivity(Intent(Intent.ACTION_VIEW, course.url.toUri()))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
            ) {
                Text(stringResource(R.string.skills_gap_course_open))
            }
        }
    }
}

/** Tarjeta blanca de una sección. */
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
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Preview(name = "Brechas · claro", widthDp = 390, heightDp = 1200)
@Composable
private fun SkillsGapPreviewLight() = SkillsGapPreview(darkTheme = false)

@Preview(name = "Brechas · oscuro", widthDp = 390, heightDp = 1200)
@Composable
private fun SkillsGapPreviewDark() = SkillsGapPreview(darkTheme = true)

@Composable
private fun SkillsGapPreview(darkTheme: Boolean) {
    CvProTheme(darkTheme = darkTheme) {
        Surface(color = MaterialTheme.colorScheme.background) {
            SkillsGapContent(
                uiState = SkillsGapUiState(
                    analysis = SkillsGapAnalysis(
                        skills = listOf(
                            SkillGapItem("Python", 70, 88),
                            SkillGapItem("SQL", 55, 73),
                            SkillGapItem("Visualización", 60, 69),
                            SkillGapItem("Machine Learning", 40, 54),
                            SkillGapItem("Cloud", 45, 60),
                            SkillGapItem("Comunicación", 75, 70),
                            SkillGapItem("Liderazgo", 65, 60),
                        ),
                        recommendations = listOf(
                            CourseRecommendation(
                                id = "1",
                                title = "Python para análisis de datos",
                                platform = "DataCamp",
                                url = "https://ejemplo.com",
                                duration = "6 horas",
                            ),
                        ),
                    ),
                ),
                onRetryClick = {},
            )
        }
    }
}
