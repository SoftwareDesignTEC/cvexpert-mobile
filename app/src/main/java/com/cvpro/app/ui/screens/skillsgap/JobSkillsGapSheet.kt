package com.cvpro.app.ui.screens.skillsgap

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cvpro.app.R
import com.cvpro.app.domain.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobSkillsGapSheet(
    offer: JobOffer,
    onDismiss: () -> Unit,
    onCorrectCv: () -> Unit,
    viewModel: JobSkillsGapViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(offer) { viewModel.open(offer) }
    DisposableEffect(viewModel) { onDispose { viewModel.close() } }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        JobSkillsGapContent(state, viewModel::loadCvs, viewModel::selectCv,
            { viewModel.analyze() }, { viewModel.analyze(evaluationOnly = true) }, onCorrectCv)
    }
}

@Composable
internal fun JobSkillsGapContent(
    state: JobSkillsGapUiState,
    onLoadCvs: () -> Unit,
    onSelectCv: (String) -> Unit,
    onAnalyze: () -> Unit,
    onEvaluateOnly: () -> Unit,
    onCorrectCv: () -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    LazyColumn(
        modifier = Modifier.fillMaxWidth().fillMaxHeight(0.95f),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(stringResource(R.string.job_gap_title), style = MaterialTheme.typography.headlineSmall)
            Text(state.offer?.title.orEmpty(), style = MaterialTheme.typography.titleMedium)
            Text(state.offer?.company.orEmpty())
            Text(stringResource(R.string.job_gap_choose_cv), modifier = Modifier.padding(top = 12.dp))
        }
        items(state.cvs, key = { "cv:" + it.id }) { cv ->
            FilterChip(
                selected = state.selectedCvId == cv.id,
                onClick = { onSelectCv(cv.id) },
                label = { Text(cv.title) },
            )
        }
        if (!state.isLoading && (state.cvPage == 0 || state.hasMoreCvs)) {
            item { OutlinedButton(onClick = onLoadCvs) { Text(stringResource(R.string.job_gap_load_cvs)) } }
        }
        if (!state.isLoading && state.cvPage > 0 && state.cvs.isEmpty()) {
            item {
                Text(stringResource(R.string.job_gap_no_cvs))
                OutlinedButton(onClick = onCorrectCv) { Text(stringResource(R.string.job_gap_open_cvs)) }
            }
        }
        if (state.isLoading) item { CircularProgressIndicator() }
        state.errorMessage?.let { message ->
            item {
                Text(stringResource(message), color = MaterialTheme.colorScheme.error)
                if (state.selectedCvId != null && state.selectedCv == null && !state.isLoading) {
                    OutlinedButton(onClick = { onSelectCv(state.selectedCvId) }) {
                        Text(stringResource(R.string.applications_retry))
                    }
                }
                if (state.needsCvCorrection) {
                    OutlinedButton(onClick = onCorrectCv) { Text(stringResource(R.string.job_gap_open_cvs)) }
                }
                if (state.canEvaluateOnly) {
                    OutlinedButton(onClick = onEvaluateOnly) { Text(stringResource(R.string.job_gap_evaluate_only)) }
                }
            }
        }
        if (state.selectedCv != null) item {
            Text(state.selectedCv.title, style = MaterialTheme.typography.titleSmall)
            Button(onClick = onAnalyze, enabled = !state.isLoading) {
                Text(stringResource(R.string.job_gap_analyze))
            }
        }
        state.analysis?.let { analysis ->
            item {
                if (analysis.catalogStatus == null) Text(stringResource(R.string.job_gap_evaluation_only))
                Text(stringResource(R.string.job_gap_client_data))
                if (analysis.status == JobCoverageStatus.INSUFFICIENT_INFORMATION) {
                    Text(stringResource(R.string.job_gap_insufficient))
                } else {
                    Text(stringResource(R.string.job_gap_coverage,
                        String.format(locale, "%.2f", analysis.documentedCoveragePercent),
                        analysis.documentedMatchesCount, analysis.requiredSkillsCount))
                }
                if (analysis.status == JobCoverageStatus.COMPLETE) Text(stringResource(R.string.job_gap_complete))
                Text(stringResource(R.string.job_gap_documented), style = MaterialTheme.typography.titleSmall)
                if (analysis.matchedSkills.isEmpty()) Text(stringResource(R.string.job_gap_no_documented))
                analysis.matchedSkills.forEach {
                    Text(stringResource(R.string.job_gap_skill_level, it.name, stringResource(it.cvLevel.levelLabel())))
                }
                Text(stringResource(R.string.job_gap_not_documented), style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 12.dp))
                if (analysis.notDocumentedSkills.isEmpty()) Text(stringResource(R.string.job_gap_no_missing))
                else Text(analysis.notDocumentedSkills.joinToString(", "))
            }
            items(analysis.recommendations, key = { "skill:" + it.skillName }) { recommendation ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.job_gap_courses_for, recommendation.skillName),
                        style = MaterialTheme.typography.titleSmall)
                    if (recommendation.courses.isEmpty()) {
                        Text(stringResource(if (recommendation.catalogMatch == SkillCatalogMatch.AMBIGUOUS)
                            R.string.job_gap_ambiguous else R.string.job_gap_no_courses))
                    }
                    recommendation.courses.forEach { course ->
                        CourseCard(course)
                        Text(stringResource(if (course.isPaid == true) R.string.job_gap_paid else R.string.job_gap_free))
                    }
                }
            }
        }
    }
}

private fun String.levelLabel(): Int = when (this) {
    "beginner" -> R.string.job_gap_level_beginner
    "intermediate" -> R.string.job_gap_level_intermediate
    "advanced" -> R.string.job_gap_level_advanced
    else -> R.string.job_gap_level_expert
}
