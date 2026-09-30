package com.cvpro.app.data.remote.dto

/**
 * Análisis de brecha tal como llega desde la API.
 *
 * Los nombres son los de SkillsGapAnalysis en docs/contract/models.ts. Este modelo
 * pertenece a la capa remota: refleja el JSON del APIM, no lo que usa la app.
 *
 * Todos los campos son opcionales a propósito. Gson no respeta los tipos no nulos de
 * Kotlin: si el JSON no trae un campo, lo deja en null igual, y la app revienta después
 * con un NullPointerException difícil de rastrear. Declararlos opcionales hace que el
 * fallo salga acá, donde el mapper puede decidir qué hacer.
 */
data class SkillsGapAnalysisDto(
    val id: String? = null,
    val userId: String? = null,
    val targetJobTitle: String? = null,
    val skills: List<SkillGapItemDto>? = null,
    val recommendations: List<CourseRecommendationDto>? = null,
    val createdAt: String? = null,
)

/** Una habilidad comparada contra el mercado. Los dos niveles van de 0 a 100. */
data class SkillGapItemDto(
    val name: String? = null,
    val userLevel: Int? = null,
    val marketLevel: Int? = null,
)

/** Curso sugerido para cerrar una brecha. */
data class CourseRecommendationDto(
    val id: String? = null,
    val title: String? = null,
    val platform: String? = null,
    val url: String? = null,
    val isPaid: Boolean? = null,
    val duration: String? = null,
)
