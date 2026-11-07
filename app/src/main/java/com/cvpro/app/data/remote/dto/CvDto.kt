package com.cvpro.app.data.remote.dto

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

/*
 * Las fechas van como JsonElement porque el servicio de CVs de la Fase II las manda en epoch
 * milisegundos (un número) y los mocks de la Fase I las mandaban como texto ISO. Con String,
 * Gson convertía el número en "1717200000000" y eso se veía en pantalla. El mapper las lee
 * con toEpochMillis(), que entiende los dos formatos. Al guardar se mandan siempre como número.
 */

data class CvDto(
    val id: String? = null,
    val userId: String? = null,
    val title: String? = null,
    val templateId: String? = null,

    // El servicio manda la misma imagen con cuatro nombres distintos. Se toma el primero
    // que venga, así da igual con cuál se quede cuando limpien el mock.
    @SerializedName(
        value = "previewUrl",
        alternate = ["templatePreviewUrl", "thumbnailUrl", "imageUrl"],
    )
    val previewUrl: String? = null,

    val createdAt: JsonElement? = null,
    val updatedAt: JsonElement? = null,
    val personalInfo: UserProfileDto? = null,
    val workExperiences: List<WorkExperienceDto>? = null,
    val education: List<EducationDto>? = null,
    val skills: List<SkillDto>? = null,
    val languages: List<LanguageDto>? = null,
    val certifications: List<CertificationDto>? = null,
)

data class WorkExperienceDto(
    val id: String? = null,
    val company: String? = null,
    val position: String? = null,
    val startDate: JsonElement? = null,
    val endDate: JsonElement? = null,
    val current: Boolean? = null,
    val description: String? = null,
    val type: String? = null,
)

data class EducationDto(
    val id: String? = null,
    val institution: String? = null,
    val degree: String? = null,
    val field: String? = null,
    val startDate: JsonElement? = null,
    val endDate: JsonElement? = null,
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

data class CertificationDto(
    val id: String? = null,
    val name: String? = null,
    val issuer: String? = null,
    val issuedAt: JsonElement? = null,
    val expiresAt: JsonElement? = null,
    val credentialUrl: String? = null,
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
    val certifications: List<CertificationDto>? = null,
)

data class UpdateCvRequestDto(
    val title: String? = null,
    val templateId: String? = null,
    val personalInfo: UserProfileDto? = null,
    val workExperiences: List<WorkExperienceDto>? = null,
    val education: List<EducationDto>? = null,
    val skills: List<SkillDto>? = null,
    val languages: List<LanguageDto>? = null,
    val certifications: List<CertificationDto>? = null,
)