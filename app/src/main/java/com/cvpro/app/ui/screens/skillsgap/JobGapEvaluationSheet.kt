package com.cvpro.app.ui.screens.skillsgap

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cvpro.app.R
import com.cvpro.app.domain.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobGapEvaluationSheet(
    offer: JobOffer,
    onDismiss: () -> Unit,
    viewModel: JobGapEvaluationViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(offer) { viewModel.open(offer) }
    DisposableEffect(viewModel) { onDispose { viewModel.close() } }
    ModalBottomSheet(onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        val visibleState = if (state.offer == offer) state else JobGapEvaluationUiState(offer = offer, isLoading = true)
        JobGapEvaluationContent(visibleState, viewModel::loadCvs, viewModel::selectCv, viewModel::evaluate, viewModel::retry)
    }
}

@Composable
internal fun JobGapEvaluationContent(
    state: JobGapEvaluationUiState,
    onLoadCvs: () -> Unit, onSelectCv: (String) -> Unit, onEvaluate: () -> Unit, onRetry: () -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.95f),
        contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(stringResource(R.string.job_gap_title), style = MaterialTheme.typography.headlineSmall)
            Text(state.offer?.title.orEmpty(), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.job_gap_choose_cv))
        }
        items(state.cvs, key = { "cv:${it.id}" }) { cv ->
            FilterChip(selected = state.selectedCvId == cv.id,
                onClick = { onSelectCv(cv.id) }, label = { Text(cv.title) })
        }
        if (state.hasMoreCvs) item {
            OutlinedButton(onClick = onLoadCvs, enabled = !state.isLoading) {
                Text(stringResource(R.string.job_gap_more_cvs))
            }
        }
        if (!state.isLoading && state.cvPage > 0 && state.cvs.isEmpty()) item {
            Text(stringResource(R.string.job_gap_no_cvs))
        }
        if (state.isLoading) item { CircularProgressIndicator() }
        state.errorMessage?.let { message -> item {
            Text(stringResource(message), color = MaterialTheme.colorScheme.error)
            OutlinedButton(onClick = onRetry, enabled = !state.isLoading) {
                Text(stringResource(R.string.applications_retry))
            }
        } }
        state.selectedCv?.let { cv -> item {
            Text(stringResource(R.string.job_gap_cv_used, cv.title), style = MaterialTheme.typography.titleSmall)
            Button(onClick = onEvaluate, enabled = !state.isLoading) {
                Text(stringResource(R.string.job_gap_analyze))
            }
        } }
        state.evaluation?.let { result ->
            item {
                Text(stringResource(R.string.job_gap_client_data))
                Text(if (result.documentedCoveragePercent == null) stringResource(R.string.job_gap_insufficient)
                    else stringResource(R.string.job_gap_coverage, result.documentedCoveragePercent.toString(),
                        result.documentedMatchesCount, result.requiredSkillsCount))
                Text(stringResource(R.string.job_gap_coverage_notice))
                Text(stringResource(R.string.job_gap_documented), style = MaterialTheme.typography.titleSmall)
                if (result.matchedSkills.isEmpty()) Text(stringResource(R.string.job_gap_none))
                result.matchedSkills.forEach { Text("${it.name}: ${stringResource(it.level.labelRes)}") }
                Text(stringResource(R.string.job_gap_not_documented_skills), style = MaterialTheme.typography.titleSmall)
                Text(result.notDocumentedSkills.joinToString(", ").ifEmpty { stringResource(R.string.job_gap_none) })
                Text(stringResource(R.string.job_gap_missing_notice))
            }
            val levels = result.levelAnalysis
            item {
                Text(stringResource(R.string.job_gap_levels), style = MaterialTheme.typography.titleMedium)
                Text(if (levels == null) stringResource(R.string.job_gap_no_levels)
                    else stringResource(R.string.job_gap_summary, levels.compared, levels.below,
                        levels.meets, levels.unknownRequired, levels.conflicts))
            }
            items(result.assessments.orEmpty()) { assessment ->
                LevelAssessmentCard(assessment)
            }
        }
    }
}

@Composable
private fun LevelAssessmentCard(assessment: JobLevelAssessment) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(assessment.name, style = MaterialTheme.typography.titleSmall)
            Text(stringResource(R.string.job_gap_cv_level, assessment.cvLevel?.let { stringResource(it.labelRes) }
                ?: stringResource(R.string.job_gap_not_documented)))
            Text(assessment.requiredLevel?.let { stringResource(R.string.job_gap_required_level, stringResource(it.labelRes)) }
                ?: stringResource(R.string.job_gap_unknown))
            Text(stringResource(when (assessment.status) {
                "documented_skill_below_required" -> R.string.job_gap_below
                "documented_skill_meets_required" -> R.string.job_gap_meets
                "not_documented" -> R.string.job_gap_not_documented
                else -> R.string.job_gap_unknown
            }))
            assessment.deficitSteps?.let { Text(stringResource(R.string.job_gap_deficit, it)) }
            if (assessment.detectionStatus == "conflict") Text(stringResource(R.string.job_gap_conflict))
            assessment.reason?.let { reason ->
                Text(stringResource(when (reason) {
                    "optional" -> R.string.job_gap_optional
                    "negated" -> R.string.job_gap_negated
                    "contradictory" -> R.string.job_gap_contradictory
                    "ambiguous" -> R.string.job_gap_ambiguous
                    else -> R.string.job_gap_no_evidence
                }))
            }
            // Backend evidence is untrusted text, never HTML or executable links.
            assessment.evidenceText?.let {
                Text(stringResource(if (assessment.evidenceSource == "description")
                    R.string.job_gap_evidence_description else R.string.job_gap_evidence_requirements, it))
            }
        }
    }
}
