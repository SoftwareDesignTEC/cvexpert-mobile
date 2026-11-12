package com.cvpro.app.ui.components

import android.content.Intent
import android.content.ActivityNotFoundException
import androidx.core.net.toUri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cvpro.app.R
import com.cvpro.app.domain.model.*

@Composable
fun CourseCard(course: Course, modifier: Modifier = Modifier) {
    if (isRealCourse(course.title, course.platform, course.url, course.skills))
        CourseCardContent(course.title, course.platform, course.url, course.durationHours, course.isPaid, course.skills, modifier)
}
@Composable
fun CourseCard(course: CourseRecommendation, modifier: Modifier = Modifier) {
    if (isRealCourse(course.title, course.platform, course.url, course.skills))
        CourseCardContent(course.title, course.platform, course.url, course.durationHours, course.isPaid, course.skills, modifier)
}
@Composable
private fun CourseCardContent(title: String, platform: String, url: String, duration: Int?, paid: Boolean?, skills: List<String>, modifier: Modifier) {
    val context = LocalContext.current
    var failed by remember(url) { mutableStateOf(false) }
    val openError = stringResource(R.string.courses_open_error)
    OutlinedCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(platform, style = MaterialTheme.typography.labelLarge)
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (skills.isNotEmpty()) Text(skills.joinToString(", "), style = MaterialTheme.typography.bodyMedium)
            duration?.let { Text(stringResource(R.string.skills_gap_course_duration, it), style = MaterialTheme.typography.bodySmall) }
            paid?.let { Text(stringResource(if (it) R.string.job_gap_paid else R.string.job_gap_free)) }
            OutlinedButton(onClick = {
                if (safeCourseUrl(url)) try { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()).addCategory(Intent.CATEGORY_BROWSABLE)) }
                catch (_: ActivityNotFoundException) { failed = true }
                catch (_: SecurityException) { failed = true }
            }, enabled = safeCourseUrl(url)) { Text(stringResource(R.string.skills_gap_course_open)) }
            if (failed) Text(openError, color = MaterialTheme.colorScheme.error)
        }
    }
}
