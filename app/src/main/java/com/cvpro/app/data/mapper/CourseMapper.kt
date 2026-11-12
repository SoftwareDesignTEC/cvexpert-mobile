package com.cvpro.app.data.mapper
import com.cvpro.app.data.remote.dto.CoursePageDto
import com.cvpro.app.domain.model.*

fun CoursePageDto.toDomain(): CoursePage {
    fun valid(value: Boolean) { if (!value) throw CourseException(CourseFailure.INVALID_RESPONSE) }
    valid(items != null && page != null && page > 0 && pageSize != null && pageSize in 1..100 && totalRecords != null && totalRecords >= 0)
    valid(items!!.size <= pageSize!! && items.map { it.id }.distinct().size == items.size)
    val courses = items.map {
        valid(!it.id.isNullOrBlank() && !it.title.isNullOrBlank() && !it.platform.isNullOrBlank() &&
            it.url != null && safeCourseUrl(it.url) && (it.logoUrl == null || safeCourseUrl(it.logoUrl)) &&
            (it.durationHours == null || it.durationHours >= 0) && it.isPaid != null && it.skills != null)
        Course(it.id!!, it.title!!, it.platform!!, it.url!!, it.logoUrl, it.durationHours, it.isPaid!!, it.skills!!)
    }
    val real = courses.filter { isRealCourse(it.title, it.platform, it.url, it.skills) }
    return CoursePage(real, page!!, pageSize, totalRecords!!, courses.size - real.size)
}
