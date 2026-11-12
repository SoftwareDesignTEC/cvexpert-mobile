package com.cvpro.app.ui.screens.skillsgap
import com.cvpro.app.domain.model.*
import com.cvpro.app.domain.repository.CourseRepository
internal class EmptyCourseRepository : CourseRepository {
    override suspend fun getCourses(page: Int, pageSize: Int, filters: CourseFilters) = CoursePage(emptyList(), page, pageSize, 0)
}
