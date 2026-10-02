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