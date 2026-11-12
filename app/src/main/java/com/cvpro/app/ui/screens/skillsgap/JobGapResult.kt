package com.cvpro.app.ui.screens.skillsgap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.cvpro.app.R
import com.cvpro.app.domain.model.*
import java.text.NumberFormat

@Composable
internal fun JobGapResult(analysis: JobSkillsGapAnalysis) {
    val presentation = remember(analysis) { JobGapPresentation(analysis) }
    val locale = LocalConfiguration.current.locales[0]
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.gap_summary), style = MaterialTheme.typography.labelLarge)
                Text(stringResource(R.string.gap_documented_count, analysis.documentedMatchesCount, analysis.requiredSkillsCount),
                    style = MaterialTheme.typography.headlineSmall)
                if (analysis.requiredSkillsCount > 0) {
                    LinearProgressIndicator(progress = { (analysis.documentedCoveragePercent!! / 100).toFloat() },
                        modifier = Modifier.fillMaxWidth().height(8.dp))
                    val percent = NumberFormat.getNumberInstance(locale).apply { minimumFractionDigits = 2; maximumFractionDigits = 2 }
                        .format(analysis.documentedCoveragePercent)
                    Text(stringResource(R.string.gap_coverage_percent, percent), style = MaterialTheme.typography.bodySmall)
                } else Text(stringResource(R.string.gap_no_identified_skills))
                Text(stringResource(R.string.gap_coverage_explained), style = MaterialTheme.typography.bodySmall)
                HorizontalDivider()
                Text(stringResource(R.string.gap_among_documented), style = MaterialTheme.typography.labelMedium)
                // FlowRow wraps naturally with large fonts instead of squeezing three fixed columns.
                FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SummaryMetric(presentation.meets.size, R.string.gap_meets_short)
                    SummaryMetric(presentation.below.size, R.string.gap_below_short)
                    SummaryMetric(presentation.unspecified.size, if (analysis.assessments == null)
                        R.string.gap_unevaluated_short else R.string.gap_unknown_short)
                }
            }
        }
        if (presentation.below.isNotEmpty()) ComparisonGroup(R.string.gap_below_group, presentation.below, MaterialTheme.colorScheme.error)
        if (presentation.meets.isNotEmpty()) ComparisonGroup(R.string.gap_meets_group, presentation.meets, MaterialTheme.colorScheme.primary)
        if (presentation.missing.isNotEmpty()) CompactSkillGroup(R.string.gap_missing_group, presentation.missingComparisons.map { skill ->
            skill.requiredLevel?.let { stringResource(R.string.gap_missing_required, skill.name, stringResource(it.labelRes)) } ?: skill.name
        })
        if (presentation.unspecified.isNotEmpty()) {
            Text(stringResource(if (analysis.assessments == null) R.string.gap_unevaluated_group
                else R.string.gap_unspecified_group), style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                presentation.unspecified.forEach { skill -> SkillPill(stringResource(R.string.gap_skill_declared,
                    skill.name, stringResource(skill.cvLevel!!.labelRes))) }
            }
        }
        AnalysisDetails(analysis)
        CourseRecommendations(presentation)
    }
}

@Composable
private fun SummaryMetric(count: Int, label: Int) {
    Column { Text(count.toString(), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(label), style = MaterialTheme.typography.labelMedium) }
}

@Composable
private fun ComparisonGroup(title: Int, skills: List<SkillComparison>, color: Color) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(title), style = MaterialTheme.typography.titleSmall, color = color)
        SkillLevelComparisonChart(if (expanded) skills else skills.take(4))
        if (skills.size > 4) TextButton(onClick = { expanded = !expanded }) {
            Text(stringResource(if (expanded) R.string.skills_gap_show_less else R.string.gap_more_skills))
        }
    }
}

/** Four discrete ordinal positions, not a proficiency percentage. Null has no bar. */
@Composable
internal fun SkillLevelComparisonChart(skills: List<SkillComparison>, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        skills.forEach { skill ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(skill.name, style = MaterialTheme.typography.bodyMedium)
                OrdinalBar(stringResource(R.string.gap_cv_series), skill.cvLevel, MaterialTheme.colorScheme.primary,
                    stringResource(R.string.gap_not_documented))
                OrdinalBar(stringResource(R.string.gap_offer_series), skill.requiredLevel, MaterialTheme.colorScheme.tertiary,
                    stringResource(R.string.gap_not_specified))
            }
        }
    }
}

@Composable
private fun OrdinalBar(label: String, level: SkillLevel?, color: Color, missing: String) {
    val text = level?.let { stringResource(it.labelRes) } ?: missing
    Row(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.widthIn(min = 48.dp))
        if (level != null) Row(Modifier.weight(1f).padding(top = 6.dp).clearAndSetSemantics {}, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(4) { index -> Box(Modifier.weight(1f).height(7.dp)
                .background(if (index < level.ordinalPosition()) color else MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(2.dp))) }
        } else Spacer(Modifier.weight(1f))
        Text(text, style = MaterialTheme.typography.labelSmall, modifier = Modifier.widthIn(max = 120.dp))
    }
}

@Composable
private fun CompactSkillGroup(title: Int, skills: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            skills.forEach { SkillPill(it) }
        }
    }
}

@Composable
private fun SkillPill(text: String) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp)) {
        Text(text, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp))
    }
}

@Composable
internal fun GapDisclosure(title: String, tag: String, content: @Composable () -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val description = stringResource(if (expanded) R.string.gap_expanded else R.string.gap_collapsed)
    OutlinedCard(Modifier.fillMaxWidth()) {
        TextButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth().testTag(tag)
            .semantics { stateDescription = description }) {
            Text(title, modifier = Modifier.weight(1f))
            Text(if (expanded) "−" else "+", modifier = Modifier.clearAndSetSemantics {})
        }
        if (expanded) Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { content() }
    }
}

@Composable
private fun AnalysisDetails(analysis: JobSkillsGapAnalysis) {
    GapDisclosure(stringResource(R.string.gap_details), "gap-details") {
        Text(stringResource(R.string.gap_method), style = MaterialTheme.typography.bodySmall)
        Text(stringResource(R.string.gap_ordinal_scale), style = MaterialTheme.typography.bodySmall)
        if (analysis.assessments == null) Text(stringResource(R.string.gap_levels_absent))
        Text(stringResource(R.string.gap_unknown_explained), style = MaterialTheme.typography.bodySmall)
        analysis.assessments.orEmpty().filter { it.evidenceText != null || it.reason in setOf("optional", "negated", "ambiguous", "contradictory") || (it.deficitSteps ?: 0) > 0 }.forEach { skill ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(skill.name, style = MaterialTheme.typography.titleSmall)
                skill.evidenceText?.let { Text(stringResource(if (skill.evidenceSource == "description")
                    R.string.gap_description_evidence else R.string.gap_requirements_evidence, it), style = MaterialTheme.typography.bodySmall) }
                skill.deficitSteps?.let { Text(stringResource(R.string.gap_ordinal_deficit, it), style = MaterialTheme.typography.bodySmall) }
                val reason = when (skill.reason) {
                    "optional" -> R.string.gap_optional_reason
                    "negated" -> R.string.gap_negated_reason
                    "ambiguous", "contradictory" -> R.string.gap_conflict_reason
                    else -> null
                }
                reason?.let { Text(stringResource(it), style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

@Composable
private fun CourseRecommendations(presentation: JobGapPresentation) {
    val independent = presentation.analysis.catalogStatus == null
    GapDisclosure(if (independent) stringResource(R.string.gap_courses_unrequested) else
        stringResource(R.string.gap_courses, presentation.courses.size), "gap-courses") {
        presentation.courses.forEach { course ->
            CourseCard(course)
            course.isPaid?.let { Text(stringResource(if (it) R.string.job_gap_paid else R.string.job_gap_free), style = MaterialTheme.typography.labelSmall) }
        }
        if (presentation.withoutCourses.isNotEmpty()) {
            Text(stringResource(R.string.gap_courses_missing_summary, presentation.withoutCourses.size), style = MaterialTheme.typography.bodyMedium)
            GapDisclosure(stringResource(R.string.gap_courses_missing_names), "gap-course-skills") {
                Text(presentation.withoutCourses.joinToString(", "), style = MaterialTheme.typography.bodySmall)
            }
        } else if (presentation.courses.isEmpty()) Text(stringResource(if (independent)
            R.string.gap_courses_not_requested else R.string.gap_courses_not_needed))
    }
}
