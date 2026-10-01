package com.cvpro.app.data.remote.dto

data class SkillsGapDto(
    val skillName: String,
    val userLevel: Int,
    val marketLevel: Int,
    val courses: List<CourseDto>,
)

data class CourseDto(
    val id: String,
    val title: String,
    val url: String,
    val platform: String,
    val logoUrl: String?,
    val durationHours: Int,
)