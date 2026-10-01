package com.cvpro.app.data.mapper

import com.cvpro.app.data.remote.dto.CvDto
import com.cvpro.app.data.remote.dto.EducationDto
import com.cvpro.app.data.remote.dto.LanguageDto
import com.cvpro.app.data.remote.dto.SkillDto
import com.cvpro.app.data.remote.dto.WorkExperienceDto
import com.cvpro.app.domain.model.Cv
import com.cvpro.app.domain.model.Education
import com.cvpro.app.domain.model.Language
import com.cvpro.app.domain.model.Skill
import com.cvpro.app.domain.model.UserProfile
import com.cvpro.app.domain.model.WorkExperience

fun CvDto.toDomain(): Cv {
    return Cv(
        id = id.orEmpty(),
        userId = userId.orEmpty(),
        title = title.orEmpty(),
        templateId = templateId.orEmpty(),
        // El servicio manda ISO 8601; las tarjetas muestran la fecha ya legible.
        createdAt = createdAt.toDisplayDate(),
        updatedAt = updatedAt.toDisplayDate(),
        // Sin datos personales el CV se muestra igual, con los campos vacíos.
        personalInfo = personalInfo?.toDomain() ?: UserProfile(
            id = "",
            name = "",
            email = "",
            phone = null,
            location = "",
            photoUrl = null,
            bio = null,
            types = emptyList(),
            modalities = emptyList(),
            sectors = emptyList(),
        ),
        workExperiences = workExperiences.orEmpty().map { it.toDomain() },
        education = education.orEmpty().map { it.toDomain() },
        skills = skills.orEmpty().map { it.toDomain() },
        languages = languages.orEmpty().map { it.toDomain() },
    )
}

private fun WorkExperienceDto.toDomain() = WorkExperience(
    id = id.orEmpty(),
    company = company.orEmpty(),
    position = position.orEmpty(),
    startDate = startDate.orEmpty(),
    endDate = endDate,
    current = current ?: false,
    description = description.orEmpty(),
    type = type.orEmpty(),
)

private fun EducationDto.toDomain() = Education(
    id = id.orEmpty(),
    institution = institution.orEmpty(),
    degree = degree.orEmpty(),
    field = field.orEmpty(),
    startDate = startDate.orEmpty(),
    endDate = endDate,
    current = current ?: false,
)

private fun SkillDto.toDomain() = Skill(
    id = id.orEmpty(),
    name = name.orEmpty(),
    level = level.orEmpty(),
    category = category.orEmpty(),
)

private fun LanguageDto.toDomain() = Language(
    id = id.orEmpty(),
    name = name.orEmpty(),
    level = level.orEmpty(),
)