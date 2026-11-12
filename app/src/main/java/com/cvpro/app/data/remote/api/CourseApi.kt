package com.cvpro.app.data.remote.api
import com.cvpro.app.data.remote.dto.CoursePageDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface CourseApi {
    @GET("courses")
    suspend fun getCourses(
        @Query("page") page: Int, @Query("pageSize") pageSize: Int,
        @Query("platform") platform: String?, @Query("isPaid") isPaid: Boolean?,
        @Query("skillName") skillName: String?,
    ): Response<CoursePageDto>
}
