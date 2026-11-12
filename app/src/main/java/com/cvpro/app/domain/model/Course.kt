package com.cvpro.app.domain.model

import java.net.URI

data class Course(
    val id: String, val title: String, val platform: String, val url: String,
    val logoUrl: String?, val durationHours: Int?, val isPaid: Boolean, val skills: List<String>,
)
data class CourseFilters(val platform: String? = null, val isPaid: Boolean? = null, val skillName: String? = null) {
    val active get() = platform != null || isPaid != null || skillName != null
}
data class CoursePage(
    val items: List<Course>, val page: Int, val pageSize: Int, val totalRecords: Long,
    val excludedRecords: Int = 0,
) {
    val hasMore get() = page.toLong() * pageSize < totalRecords
}
enum class CourseFailure { AUTHENTICATION, ROUTE_UNAVAILABLE, CONNECTION, UNAVAILABLE, INVALID_RESPONSE }
class CourseException(val failure: CourseFailure) : Exception(failure.name)

fun safeCourseUrl(url: String): Boolean = runCatching {
    val uri = URI(url)
    uri.scheme in setOf("https", "http") && !uri.host.isNullOrBlank() && uri.userInfo == null
}.getOrDefault(false)

/** Presentation guard; counts from PostgreSQL are never rewritten. No course data is logged. */
fun isRealCourse(title: String, platform: String, url: String, skills: List<String>): Boolean {
    if (!safeCourseUrl(url)) return false
    if ((listOf(title, platform) + skills).any { "[DEMO]" in it.uppercase() || "[TEST]" in it.uppercase() }) return false
    val host = URI(url).host.lowercase()
    return listOf("example.com", "example.org", "example.net", "example.edu", "example.invalid", "localhost").none {
        host == it || host.endsWith(".$it")
    } && !host.endsWith(".example") && !host.endsWith(".test") && !host.endsWith(".invalid")
}
fun JobLevelAssessment.canSuggestLevelCourses(): Boolean =
    status == "documented_skill_below_required" && detectionStatus == "explicit" &&
        (deficitSteps ?: 0) > 0 && requiredLevel != null && cvLevel != null
