package com.cvpro.app.data.remote.dto

data class CoursePageDto(
    val items: List<CatalogCourseDto>?, val page: Int?, val pageSize: Int?, val totalRecords: Long?,
)
data class CatalogCourseDto(
    val id: String?, val title: String?, val platform: String?, val url: String?,
    val logoUrl: String?, val durationHours: Int?, val isPaid: Boolean?, val skills: List<String>?,
)
