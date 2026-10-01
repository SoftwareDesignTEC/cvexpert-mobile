package com.cvpro.app.data.remote.dto

data class CvDto(
    val id: String? = null,
    val userId: String? = null,
    val title: String? = null,
    val templateId: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val personalInfo: UserProfileDto? = null,
    val workExperiences: List<WorkExperienceDto>? = null,
    val education: List<EducationDto>? = null,
    val skills: List<SkillDto>? = null,
    val languages: List<LanguageDto>? = null,
)

data class WorkExperienceDto(
    val id: String? = null,
    val company: String? = null,
    val position: String? = null,
    val startDate: String? = null,
    val endDate: String?,
    val current: Boolean? = null,
    val description: String? = null,
    val type: String? = null,
)

data class EducationDto(
    val id: String? = null,
    val institution: String? = null,
    val degree: String? = null,
    val field: String? = null,
    val startDate: String? = null,
    val endDate: String?,
    val current: Boolean? = null,
)

data class SkillDto(
    val id: String? = null,
    val name: String? = null,
    val level: String? = null,
    val category: String? = null,
)

data class LanguageDto(
    val id: String? = null,
    val name: String? = null,
    val level: String? = null,
)

data class CreateCvRequestDto(
    val userId: String? = null,
    val title: String? = null,
    val templateId: String? = null,
    val personalInfo: UserProfileDto? = null,
    val workExperiences: List<WorkExperienceDto>? = null,
    val education: List<EducationDto>? = null,
    val skills: List<SkillDto>? = null,
    val languages: List<LanguageDto>? = null,
)

data class UpdateCvRequestDto(
    val title: String? = null,
    val templateId: String? = null,
    val personalInfo: UserProfileDto? = null,
    val workExperiences: List<WorkExperienceDto>? = null,
    val education: List<EducationDto>? = null,
    val skills: List<SkillDto>? = null,
    val languages: List<LanguageDto>? = null,
)