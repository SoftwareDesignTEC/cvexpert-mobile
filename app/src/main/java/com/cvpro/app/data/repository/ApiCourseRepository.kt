package com.cvpro.app.data.repository
import com.cvpro.app.data.remote.api.CourseApi
import com.cvpro.app.data.mapper.toDomain
import com.cvpro.app.domain.model.*
import com.cvpro.app.domain.repository.CourseRepository
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

class ApiCourseRepository @Inject constructor(private val api: CourseApi) : CourseRepository {
    override suspend fun getCourses(page: Int, pageSize: Int, filters: CourseFilters): CoursePage {
        require(page > 0 && pageSize in 1..100)
        try {
            val response = api.getCourses(page, pageSize, filters.platform, filters.isPaid, filters.skillName)
            if (!response.isSuccessful) throw CourseException(when (response.code()) {
                401, 403 -> CourseFailure.AUTHENTICATION
                404, 405 -> CourseFailure.ROUTE_UNAVAILABLE
                else -> CourseFailure.UNAVAILABLE
            })
            val result = response.body()?.toDomain() ?: throw CourseException(CourseFailure.INVALID_RESPONSE)
            if (result.page != page || result.pageSize != pageSize) throw CourseException(CourseFailure.INVALID_RESPONSE)
            return result
        } catch (error: CancellationException) { throw error }
        catch (error: IOException) { throw CourseException(CourseFailure.CONNECTION) }
    }
}
