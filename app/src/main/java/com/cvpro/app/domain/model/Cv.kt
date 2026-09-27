package com.cvpro.app.domain.model

data class Cv(
    val id: String,
    val userId: String,
    val title: String,
    val templateId: String,
    val createdAt: String,
    val updatedAt: String,
    val personalInfo: UserProfile,
    val workExperiences: List<WorkExperience>,
    val education: List<Education>,
    val skills: List<Skill>,
    val languages: List<Language>,
)

data class WorkExperience(
    val id: String,
    val company: String,
    val position: String,
    val startDate: String,
    val endDate: String?,
    val current: Boolean,
    val description: String,
    val type: String,
)

data class Education(
    val id: String,
    val institution: String,
    val degree: String,
    val field: String,
    val startDate: String,
    val endDate: String?,
    val current: Boolean,
)

data class Skill(
    val id: String,
    val name: String,
    val level: String,
    val category: String,
)

data class Language(
    val id: String,
    val name: String,
    val level: String,
)