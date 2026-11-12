package com.cvpro.app.ui.screens.courses

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cvpro.app.R
import com.cvpro.app.domain.model.*
import com.cvpro.app.ui.components.CourseCard

@Composable
fun CoursesScreen(skillName: String? = null, viewModel: CoursesViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(skillName) { viewModel.start(skillName) }
    CoursesContent(state, viewModel::applyFilters, viewModel::load, viewModel::retry)
}
@Composable
internal fun CoursesContent(state: CoursesUiState, onFilters: (CourseFilters) -> Unit, onPage: (Int) -> Unit, onRetry: () -> Unit) {
    var platform by rememberSaveable(state.filters) { mutableStateOf(state.filters.platform.orEmpty()) }
    var skill by rememberSaveable(state.filters) { mutableStateOf(state.filters.skillName.orEmpty()) }
    var paid by rememberSaveable(state.filters) { mutableStateOf(state.filters.isPaid) }
    LazyColumn(modifier = Modifier.fillMaxSize().testTag("courses-catalog"), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.courses_explore), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.courses_exact_filters), style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(platform, { platform = it }, label = { Text(stringResource(R.string.courses_platform)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(skill, { skill = it }, label = { Text(stringResource(R.string.courses_skill)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(null, false, true).forEach { value ->
                        FilterChip(selected = paid == value, onClick = { paid = value }, label = {
                            Text(stringResource(when(value) { null -> R.string.courses_any_price; false -> R.string.job_gap_free; true -> R.string.job_gap_paid }))
                        })
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onFilters(CourseFilters(platform.takeIf { it.isNotBlank() }, paid, skill.takeIf { it.isNotBlank() })) }) { Text(stringResource(R.string.courses_apply)) }
                    TextButton(onClick = { onFilters(CourseFilters()) }) { Text(stringResource(R.string.courses_clear)) }
                }
            }
        }
        if (state.loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        state.error?.let { error -> item { CourseError(error, onRetry) } }
        state.result?.let { result ->
            item { Text(stringResource(R.string.courses_page, result.page, result.totalRecords)) }
            if (result.excludedRecords > 0) item { Text(stringResource(R.string.courses_data_defect), color = MaterialTheme.colorScheme.error) }
            if (result.items.isEmpty() && !state.loading && state.error == null) item {
                Text(stringResource(if (result.totalRecords > 0) R.string.courses_page_empty else if (state.filters.active) R.string.courses_no_matches else R.string.courses_empty))
            }
            items(result.items, key = { it.id }) { CourseCard(it) }
            item { FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (result.page > 1) OutlinedButton(onClick = { onPage(result.page - 1) }, enabled = !state.loading) { Text(stringResource(R.string.skills_gap_previous)) }
                if (result.hasMore) OutlinedButton(onClick = { onPage(result.page + 1) }, enabled = !state.loading) { Text(stringResource(R.string.skills_gap_next)) }
            } }
        }
    }
}
@Composable
internal fun CourseError(error: CourseFailure, onRetry: () -> Unit) {
    Column {
        Text(stringResource(when(error) {
            CourseFailure.AUTHENTICATION -> R.string.skills_gap_unauthorized
            CourseFailure.ROUTE_UNAVAILABLE -> R.string.job_gap_route_unavailable
            CourseFailure.CONNECTION -> R.string.job_gap_network_error
            CourseFailure.INVALID_RESPONSE -> R.string.job_gap_incompatible
            CourseFailure.UNAVAILABLE -> R.string.courses_unavailable
        }), color = MaterialTheme.colorScheme.error)
        TextButton(onClick = onRetry) { Text(stringResource(R.string.applications_retry)) }
    }
}
