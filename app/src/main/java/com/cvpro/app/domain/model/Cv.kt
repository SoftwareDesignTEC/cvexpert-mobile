package com.cvpro.app.domain.model

data class Cv(
    val id: String,
    val userId: String,
    val title: String,
    val templateId: String,
    val createdAt: String,
    val updatedAt: String,
    // OJO: previewUrl NO está en el contrato de la web (docs/contract/models.ts). Lo manda
    // el APIM y es la imagen del currículum ya armado, que es lo que dibuja la tarjeta.
    // Va como opcional para no romper la paridad: si el servicio deja de mandarla, la
    // tarjeta vuelve sola a la vista previa dibujada a mano.
    // TODO: pedirle a Pérez que lo agregue al contrato, o a Cabrera que lo saque.
    val previewUrl: String?,
    val personalInfo: UserProfile,
    val workExperiences: List<WorkExperience>,
    val education: List<Education>,
    val skills: List<Skill>,
    val languages: List<Language>,
    val certifications: List<Certification>,
)

/*
 * Las fechas de experiencia, estudios y certificaciones son epoch milisegundos UTC, igual que
 * en el contrato del servicio de CVs (ver CvDates.kt para cómo se muestran). Son null cuando
 * el servicio no manda una fecha que se pueda leer o el usuario todavía no la eligió.
 */

data class WorkExperience(
    val id: String,
    val company: String,
    val position: String,
    val startDate: Long?,
    val endDate: Long?,
    val current: Boolean,
    val description: String,
    val type: String,
)

data class Education(
    val id: String,
    val institution: String,
    val degree: String,
    val field: String,
    val startDate: Long?,
    val endDate: Long?,
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

/** Certificación o curso con constancia. Es un agregado de la Fase II y es opcional en el CV. */
data class Certification(
    val id: String,
    val name: String,
    val issuer: String,
    val issuedAt: Long?,
    val expiresAt: Long?,
    val credentialUrl: String?,
)
