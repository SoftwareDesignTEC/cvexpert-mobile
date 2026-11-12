package com.cvpro.app.domain.repository
import com.cvpro.app.domain.model.*
interface CourseRepository {
    suspend fun getCourses(page: Int = 1, pageSize: Int = 20, filters: CourseFilters = CourseFilters()): CoursePage
}
