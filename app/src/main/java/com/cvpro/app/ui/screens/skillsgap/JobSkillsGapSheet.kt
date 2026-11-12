package com.cvpro.app.ui.screens.skillsgap

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cvpro.app.R
import com.cvpro.app.domain.model.JobOffer

/** The Jobs entry keeps its offer fixed. The main screen uses exactly the same content and VM. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobSkillsGapSheet(
    offer: JobOffer, onDismiss: () -> Unit, onCorrectCv: () -> Unit,
    viewModel: JobSkillsGapViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(offer) { viewModel.open(offer) }
    DisposableEffect(viewModel) { onDispose { viewModel.close() } }
    ModalBottomSheet(onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        val visible = if (state.offer == offer) state else JobSkillsGapUiState(offer = offer, offerLocked = true, isLoading = true)
        JobSkillsGapContent(visible, viewModel::loadCvs, viewModel::selectCv, { viewModel.analyze() },
            { viewModel.analyze(true) }, onCorrectCv, onRetry = viewModel::retry,
            modifier = Modifier.fillMaxHeight(0.95f))
    }
}

@Composable
internal fun JobSkillsGapOfferScreen(
    onCorrectCv: () -> Unit, modifier: Modifier = Modifier,
    viewModel: JobSkillsGapViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.start() }
    JobSkillsGapContent(state, viewModel::loadCvs, viewModel::selectCv, { viewModel.analyze() },
        { viewModel.analyze(true) }, onCorrectCv, modifier = modifier, onRetry = viewModel::retry,
        onOfferQuery = viewModel::changeOfferQuery, onLoadOffers = viewModel::loadOffers, onSelectOffer = viewModel::selectOffer)
}

@Composable
internal fun JobSkillsGapContent(
    state: JobSkillsGapUiState,
    onLoadCvs: () -> Unit, onSelectCv: (String) -> Unit,
    onAnalyze: () -> Unit, onEvaluateOnly: () -> Unit, onCorrectCv: () -> Unit,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit = onAnalyze,
    onOfferQuery: (String) -> Unit = {}, onLoadOffers: () -> Unit = {}, onSelectOffer: (String) -> Unit = {},
) {
    var editCv by rememberSaveable { mutableStateOf(false) }
    var editOffer by rememberSaveable { mutableStateOf(false) }
    val showCvs = state.selectedCv == null || editCv
    val showOffers = state.selectedCv != null && !state.offerLocked && (state.offer == null || editOffer)
    LazyColumn(modifier = modifier.fillMaxSize().testTag("job-gap-flow"),
        contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(state.offer?.title ?: stringResource(R.string.gap_offer_intro), style = MaterialTheme.typography.headlineSmall)
                state.offer?.let { Text(it.company, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant) }
                state.selectedCv?.let { Text(stringResource(R.string.job_gap_cv_used, it.title), style = MaterialTheme.typography.bodyMedium) }
                if (state.selectedCv != null) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { editCv = !editCv }) { Text(stringResource(R.string.gap_change_cv)) }
                    if (!state.offerLocked && state.offer != null) TextButton(onClick = { editOffer = !editOffer }) {
                        Text(stringResource(R.string.gap_change_offer))
                    }
                }
            }
        }
        if (showCvs) {
            item { Text(stringResource(R.string.gap_choose_cv), style = MaterialTheme.typography.titleMedium) }
            items(state.cvs, key = { "cv:${it.id}" }) { cv ->
                OutlinedCard(onClick = { editCv = false; onSelectCv(cv.id) }, modifier = Modifier.fillMaxWidth()) {
                    Text(cv.title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(16.dp))
                }
            }
            if (state.hasMoreCvs) item {
                OutlinedButton(onClick = onLoadCvs, enabled = !state.isLoading) { Text(stringResource(R.string.job_gap_load_cvs)) }
            }
            if (!state.isLoading && state.cvPage > 0 && state.cvs.isEmpty()) item {
                Text(stringResource(R.string.job_gap_no_cvs))
                TextButton(onClick = onCorrectCv) { Text(stringResource(R.string.job_gap_open_cvs)) }
            }
        }
        if (showOffers) {
            item {
                Text(stringResource(R.string.gap_choose_offer), style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(value = state.offerQuery, onValueChange = onOfferQuery,
                    label = { Text(stringResource(R.string.gap_search_offers)) }, singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("gap-offer-search"))
            }
            items(state.offers, key = { "offer:${it.id}" }) { offer ->
                OutlinedCard(onClick = { editOffer = false; onSelectOffer(offer.id) }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(offer.title, style = MaterialTheme.typography.titleSmall)
                        Text(offer.company, style = MaterialTheme.typography.bodyMedium)
                        if (offer.location.isNotBlank()) Text(offer.location, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (state.isLoadingOffers) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            state.offersError?.let { message -> item {
                Text(stringResource(message), color = MaterialTheme.colorScheme.error)
                TextButton(onClick = onLoadOffers) { Text(stringResource(R.string.applications_retry)) }
            } }
            if (!state.isLoadingOffers && state.offersError == null && state.offerPage > 0 && state.offers.isEmpty()) item {
                Text(stringResource(R.string.gap_no_offers))
            }
            if (state.hasMoreOffers && !state.isLoadingOffers) item {
                OutlinedButton(onClick = onLoadOffers) { Text(stringResource(R.string.gap_more_offers)) }
            }
        }
        if (state.isLoading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        state.errorMessage?.let { message -> item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.gap_error_title), style = MaterialTheme.typography.titleSmall)
                    Text(stringResource(message), style = MaterialTheme.typography.bodyMedium)
                    if (!state.isLoading) TextButton(onClick = onRetry) { Text(stringResource(R.string.applications_retry)) }
                    if (state.canEvaluateOnly) OutlinedButton(onClick = onEvaluateOnly, enabled = !state.isLoading) {
                        Text(stringResource(R.string.gap_only_levels))
                    }
                    if (state.needsCvCorrection) TextButton(onClick = onCorrectCv) { Text(stringResource(R.string.job_gap_open_cvs)) }
                }
            }
        } }
        if (state.selectedCv != null && state.offer != null && state.analysis == null && state.errorMessage == null) item {
            Button(onClick = onAnalyze, enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth().testTag("gap-analyze")) {
                Text(stringResource(R.string.gap_analyze_skills))
            }
        }
        state.analysis?.let { analysis -> item(key = "result:${analysis.cvId}:${analysis.jobId}") {
            JobGapResult(analysis)
        } }
    }
}
