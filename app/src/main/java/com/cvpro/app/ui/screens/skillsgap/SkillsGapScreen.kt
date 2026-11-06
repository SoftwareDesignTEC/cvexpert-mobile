package com.cvpro.app.ui.screens.skillsgap

import android.content.Intent
import androidx.core.net.toUri
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.cvpro.app.ui.components.RemoteImage
import com.cvpro.app.R
import com.cvpro.app.ui.components.outlinedOnCardColors
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
        onSelectCv = viewModel::onSelectCv,
        onAnalyze = viewModel::onAnalyzeClick,
        onMoreCvs = viewModel::onMoreCvs,
        onPage = viewModel::onPage,
        modifier = modifier,
    )
}

@Composable
fun SkillsGapContent(
    uiState: SkillsGapUiState,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
    onSelectCv: (String) -> Unit = {},
    onAnalyze: () -> Unit = {},
    onMoreCvs: () -> Unit = {},
    onPage: (Int) -> Unit = {},
) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (!uiState.enabled) {
            Text(stringResource(R.string.skills_gap_disabled))
            return@Column
        }
        Text(stringResource(R.string.skills_gap_select_cv), style = MaterialTheme.typography.titleMedium)
        uiState.cvErrorMessage?.let { Text(stringResource(it)) }
        uiState.cvs.forEach { cv ->
            OutlinedButton(onClick = { onSelectCv(cv.id) }, enabled = !uiState.isLoading) {
                Text(cv.title)
            }
        }
        if (uiState.hasMoreCvs) {
            OutlinedButton(onClick = onMoreCvs, enabled = !uiState.isLoading) {
                Text(stringResource(R.string.skills_gap_more_cvs))
            }
        }
        if (uiState.cvs.isEmpty() && uiState.cvPage > 0 && !uiState.isLoading) Text(stringResource(R.string.skills_gap_no_cvs))
        uiState.selectedCv?.let { cv ->
            Text(stringResource(R.string.skills_gap_selected_cv, cv.title))
            Text(stringResource(R.string.skills_gap_replace_notice, uiState.input.orEmpty().size,
                uiState.input.orEmpty().count { it.level == null }))
            Button(onClick = onAnalyze, enabled = !uiState.isLoading && uiState.input != null) {
                Text(stringResource(R.string.skills_gap_analyze))
            }
        }
        if (uiState.isLoading) CircularProgressIndicator()
        uiState.errorMessage?.let { Text(stringResource(it)) }
        OutlinedButton(onClick = onRetryClick, enabled = !uiState.isLoading) {
            Text(stringResource(R.string.applications_retry))
        }
        uiState.analysis?.let { analysis ->
            Text(stringResource(R.string.skills_gap_snapshot_notice))
            SkillsGapSections(analysis)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (analysis.page > 1) OutlinedButton(onClick = { onPage(analysis.page - 1) }, enabled = !uiState.isLoading) {
                    Text(stringResource(R.string.skills_gap_previous))
                }
                Text(stringResource(R.string.skills_gap_page, analysis.page, analysis.totalRecords))
                if (analysis.hasMore) OutlinedButton(onClick = { onPage(analysis.page + 1) }, enabled = !uiState.isLoading) {
                    Text(stringResource(R.string.skills_gap_next))
                }
            }
        }
    }
}

@Composable
private fun SkillsGapSections(
    analysis: SkillsGapAnalysis,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.skills_gap_ordinal_scale))
        Text(stringResource(R.string.skills_gap_deficits_only))
        Text(stringResource(R.string.skills_gap_coverage, analysis.comparedSkills, analysis.unresolved.size))
        if (analysis.marketStatus == "missing") Text(stringResource(R.string.skills_gap_market_missing))
        if (analysis.status != "complete" || analysis.unresolved.isNotEmpty() || analysis.comparedSkills == 0)
            Text(stringResource(R.string.skills_gap_incomplete))
        analysis.unresolved.forEach { skill ->
            Text("${skill.skillName}: ${stringResource(unresolvedReasonMessage(skill.reason))}")
        }
        if (analysis.skills.isNotEmpty()) ComparisonCard(analysis = analysis)
        if (analysis.skills.isNotEmpty() || analysis.canConfirmNoGaps)
            ImprovementPointsCard(skills = analysis.skillsToImprove)
        CoursesCard(courses = analysis.recommendations)
        Spacer(modifier = Modifier.height(8.dp))
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
            // Se muestran las tres de mayor brecha; el resto queda detrás del botón.
            var showAll by remember { mutableStateOf(false) }
            val visible = if (showAll) skills else skills.take(PREVIEW_COUNT)

            visible.forEachIndexed { index, skill ->
                ImprovementRow(position = index + 1, skill = skill)
            }

            if (skills.size > PREVIEW_COUNT) {
                ExpandButton(
                    isExpanded = showAll,
                    hiddenCount = skills.size - PREVIEW_COUNT,
                    onClick = { showAll = !showAll },
                )
            }
        }
    }
}

/**
 * Botón para ver el resto de una lista recortada.
 *
 * Dice cuántos faltan en vez de un "Ver todos" a secas: así se sabe si vale la pena
 * abrirlo antes de tocarlo.
 */
@Composable
private fun ExpandButton(
    isExpanded: Boolean,
    hiddenCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        colors = outlinedOnCardColors(),
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
    ) {
        Text(
            text = if (isExpanded) {
                stringResource(R.string.skills_gap_show_less)
            } else {
                stringResource(R.string.skills_gap_show_all, hiddenCount)
            },
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

/** Cuántos elementos se ven antes de tocar "Ver todos". */
private const val PREVIEW_COUNT = 3

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
            // Barrita de color pegada al borde izquierdo de la fila.
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
                    // Rojo y no azul: estas son las habilidades que están por debajo de lo
                    // que pide el mercado, o sea lo que hay que atender.
                    .background(MaterialTheme.colorScheme.error),
            )
            Text(
                text = stringResource(R.string.skills_gap_row_number, position),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 10.dp),
            )
            Text(
                text = "${skill.name} (${skill.userLevel} → ${skill.marketLevel})",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
            )
            Text(
                text = stringResource(R.string.skills_gap_deficit_steps, skill.deficitSteps),
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

        // Mismo recorte que los puntos a mejorar: tres y el resto detrás del botón.
        var showAll by remember { mutableStateOf(false) }
        val visible = if (showAll) courses else courses.take(PREVIEW_COUNT)

        visible.forEach { course ->
            CourseCard(course = course, modifier = Modifier.padding(top = 12.dp))
        }

        if (courses.size > PREVIEW_COUNT) {
            ExpandButton(
                isExpanded = showAll,
                hiddenCount = courses.size - PREVIEW_COUNT,
                onClick = { showAll = !showAll },
            )
        }
    }
}

@Composable
private fun CourseCard(
    course: CourseRecommendation,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    // Fondo y borde propios: la tarjeta de la sección también es blanca, así que sin esto
    // los cursos quedaban pegados entre sí, sin separación visible.
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    RemoteImage(
                        url = course.logoUrl,
                        contentDescription = "Logo de ${course.platform}",
                        modifier = Modifier.fillMaxSize(),
                        fallback = {
                            Text(
                                text = course.platform.take(1).uppercase(),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        },
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
                    text = course.duration?.let {
                        stringResource(R.string.skills_gap_course_duration, it)
                    } ?: stringResource(R.string.skills_gap_course_no_duration),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Button(
                onClick = {
                    context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            course.url.toUri(),
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
            ) {
                Text(
                    stringResource(R.string.skills_gap_course_open)
                )
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
                    enabled = true,
                    analysis = SkillsGapAnalysis(
                        skills = listOf(
                            SkillGapItem("Python", 1, 3),
                            SkillGapItem("SQL", 1, 3),
                            SkillGapItem("Visualización", 1, 3),
                            SkillGapItem("Machine Learning", 1, 3),
                            SkillGapItem("Cloud", 1, 3),
                            SkillGapItem("Comunicación", 1, 3),
                            SkillGapItem("Liderazgo", 1, 3),
                        ),
                        recommendations = listOf(
                            CourseRecommendation(
                                id = "1",
                                title = "Python para análisis de datos",
                                platform = "DataCamp",
                                url = "https://ejemplo.com",
                                logoUrl = null,
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

internal fun unresolvedReasonMessage(reason: String): Int = when (reason) {
    "MISSING_USER_SKILL" -> R.string.skills_gap_missing_user_skill
    "MISSING_MARKET_REQUIREMENT" -> R.string.skills_gap_missing_requirement
    "UNKNOWN_LEVEL" -> R.string.skills_gap_unknown_level
    "UNKNOWN_SCALE" -> R.string.skills_gap_unknown_scale
    "SCALE_MISMATCH" -> R.string.skills_gap_scale_mismatch
    else -> R.string.skills_gap_unresolved_other
}
