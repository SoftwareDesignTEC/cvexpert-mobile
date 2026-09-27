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
import com.cvpro.app.domain.model.WorkExperience

fun CvDto.toDomain(): Cv {
    return Cv(
        id = id,
        userId = userId,
        title = title,
        templateId = templateId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        personalInfo = personalInfo.toDomain(),
        workExperiences = workExperiences.map { it.toDomain() },
        education = education.map { it.toDomain() },
        skills = skills.map { it.toDomain() },
        languages = languages.map { it.toDomain() },
    )
}

private fun WorkExperienceDto.toDomain() = WorkExperience(
    id = id,
    company = company,
    position = position,
    startDate = startDate,
    endDate = endDate,
    current = current,
    description = description,
    type = type,
)

private fun EducationDto.toDomain() = Education(
    id = id,
    institution = institution,
    degree = degree,
    field = field,
    startDate = startDate,
    endDate = endDate,
    current = current,
)

private fun SkillDto.toDomain() = Skill(
    id = id,
    name = name,
    level = level,
    category = category,
)

private fun LanguageDto.toDomain() = Language(
    id = id,
    name = name,
    level = level,
)