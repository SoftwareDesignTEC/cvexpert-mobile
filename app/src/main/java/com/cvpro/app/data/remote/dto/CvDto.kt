package com.cvpro.app.data.remote.dto

data class CvDto(
    val id: String,
    val userId: String,
    val title: String,
    val templateId: String,
    val createdAt: String,
    val updatedAt: String,
    val personalInfo: UserProfileDto,
    val workExperiences: List<WorkExperienceDto>,
    val education: List<EducationDto>,
    val skills: List<SkillDto>,
    val languages: List<LanguageDto>,
)

data class WorkExperienceDto(
    val id: String,
    val company: String,
    val position: String,
    val startDate: String,
    val endDate: String?,
    val current: Boolean,
    val description: String,
    val type: String,
)

data class EducationDto(
    val id: String,
    val institution: String,
    val degree: String,
    val field: String,
    val startDate: String,
    val endDate: String?,
    val current: Boolean,
)

data class SkillDto(
    val id: String,
    val name: String,
    val level: String,
    val category: String,
)

data class LanguageDto(
    val id: String,
    val name: String,
    val level: String,
)

data class CreateCvRequestDto(
    val userId: String,
    val title: String,
    val templateId: String,
    val personalInfo: UserProfileDto,
    val workExperiences: List<WorkExperienceDto>,
    val education: List<EducationDto>,
    val skills: List<SkillDto>,
    val languages: List<LanguageDto>,
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