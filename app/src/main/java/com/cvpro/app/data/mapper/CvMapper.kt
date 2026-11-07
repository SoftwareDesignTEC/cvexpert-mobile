package com.cvpro.app.data.mapper

import com.cvpro.app.data.remote.dto.CertificationDto
import com.cvpro.app.data.remote.dto.CreateCvRequestDto
import com.cvpro.app.data.remote.dto.CvDto
import com.cvpro.app.data.remote.dto.EducationDto
import com.cvpro.app.data.remote.dto.LanguageDto
import com.cvpro.app.data.remote.dto.SkillDto
import com.cvpro.app.data.remote.dto.UpdateCvRequestDto
import com.cvpro.app.data.remote.dto.UserProfileDto
import com.cvpro.app.data.remote.dto.WorkExperienceDto
import com.cvpro.app.domain.model.Certification
import com.cvpro.app.domain.model.Cv
import com.cvpro.app.domain.model.Education
import com.cvpro.app.domain.model.JobModality
import com.cvpro.app.domain.model.Language
import com.cvpro.app.domain.model.Skill
import com.cvpro.app.domain.model.UserProfile
import com.cvpro.app.domain.model.WorkExperience
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive

fun CvDto.toDomain(): Cv {
    return Cv(
        id = id.orEmpty(),
        userId = userId.orEmpty(),
        title = title.orEmpty(),
        templateId = templateId.orEmpty(),
        // Llegan en epoch (o ISO en los mocks viejos); las tarjetas muestran la fecha legible.
        createdAt = createdAt.toEpochMillis().toDisplayDate(),
        updatedAt = updatedAt.toEpochMillis().toDisplayDate(),
        previewUrl = previewUrl,
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
        certifications = certifications.orEmpty().map { it.toDomain() },
    )
}

private fun WorkExperienceDto.toDomain() = WorkExperience(
    id = id.orEmpty(),
    company = company.orEmpty(),
    position = position.orEmpty(),
    startDate = startDate.toEpochMillis(),
    endDate = endDate.toEpochMillis(),
    current = current ?: false,
    description = description.orEmpty(),
    type = type.orEmpty(),
)

private fun EducationDto.toDomain() = Education(
    id = id.orEmpty(),
    institution = institution.orEmpty(),
    degree = degree.orEmpty(),
    field = field.orEmpty(),
    startDate = startDate.toEpochMillis(),
    endDate = endDate.toEpochMillis(),
    current = current ?: false,
)

private fun CertificationDto.toDomain() = Certification(
    id = id.orEmpty(),
    name = name.orEmpty(),
    issuer = issuer.orEmpty(),
    issuedAt = issuedAt.toEpochMillis(),
    expiresAt = expiresAt.toEpochMillis(),
    credentialUrl = credentialUrl?.takeIf { it.isNotBlank() },
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
/**
 * Pasa el currículum editado al cuerpo que espera `PUT /cvs/{id}`.
 *
 * Es el camino inverso al de arriba: el dominio usa `name` y `location`, y el servicio
 * espera `fullName` y `address`. Van todas las secciones, porque el PUT reemplaza el
 * currículum completo y no solo los campos que cambiaron.
 */
fun Cv.toUpdateRequest(): UpdateCvRequestDto {
    return UpdateCvRequestDto(
        title = title,
        templateId = templateId,
        personalInfo = personalInfo.toDto(),
        workExperiences = workExperiences.map { it.toDto() },
        education = education.map { it.toDto() },
        skills = skills.map { it.toDto() },
        languages = languages.map { it.toDto() },
        // Aunque la app no edita certificaciones, se devuelven tal cual: el PUT reemplaza el
        // CV entero y, si no se mandaran, se borrarían las que se cargaron desde la web.
        certifications = certifications.map { it.toDto() },
    )
}

/** Cuerpo de `POST /cvs` para el currículum que nace de una plantilla. */
fun newCvRequest(
    title: String,
    templateId: String,
    personalInfo: UserProfile?,
): CreateCvRequestDto {
    return CreateCvRequestDto(
        userId = personalInfo?.id,
        title = title,
        templateId = templateId,
        personalInfo = personalInfo?.toDto(),
        workExperiences = emptyList(),
        education = emptyList(),
        skills = emptyList(),
        languages = emptyList(),
        certifications = emptyList(),
    )
}

private fun UserProfile.toDto() = UserProfileDto(
    id = id,
    fullName = name,
    email = email,
    phone = phone,
    address = location,
    photoUrl = photoUrl,
    bio = bio,
    types = types,
    modalities = modalities.map { it.toApiValue() },
    sectors = sectors,
)

/** Los mismos textos que el servicio manda al leer, para que el ida y vuelta cierre. */
private fun JobModality.toApiValue(): String = when (this) {
    JobModality.REMOTE -> "remote"
    JobModality.ON_SITE -> "on-site"
    JobModality.HYBRID -> "hybrid"
}

private fun WorkExperience.toDto() = WorkExperienceDto(
    id = id.takeIf { it.isNotBlank() },
    company = company,
    position = position,
    startDate = startDate.toJson(),
    endDate = endDate.toJson(),
    current = current,
    description = description,
    type = type,
)

private fun Education.toDto() = EducationDto(
    id = id.takeIf { it.isNotBlank() },
    institution = institution,
    degree = degree,
    field = field,
    startDate = startDate.toJson(),
    endDate = endDate.toJson(),
    current = current,
)

private fun Skill.toDto() = SkillDto(
    id = id.takeIf { it.isNotBlank() },
    name = name,
    level = level,
    category = category,
)

private fun Language.toDto() = LanguageDto(
    id = id.takeIf { it.isNotBlank() },
    name = name,
    level = level,
)

private fun Certification.toDto() = CertificationDto(
    id = id.takeIf { it.isNotBlank() },
    name = name,
    issuer = issuer,
    issuedAt = issuedAt.toJson(),
    expiresAt = expiresAt.toJson(),
    credentialUrl = credentialUrl,
)

/** Epoch → número JSON, que es lo que pide el contrato al guardar. Sin fecha, no se manda. */
private fun Long?.toJson(): JsonElement? = this?.let { JsonPrimitive(it) }
