package com.cvpro.app.domain.model

/**
 * Una habilidad comparada contra lo que piden las ofertas del puesto al que apuntás.
 *
 * Los dos niveles van de 0 a 100. La brecha es cuánto le falta a tu nivel para alcanzar el
 * del mercado; si tu nivel ya es igual o mayor, no hay brecha.
 */
data class SkillGapItem(
    val name: String,
    val userLevel: Int,
    val marketLevel: Int,
) {
    val gap: Int = (marketLevel - userLevel).coerceAtLeast(0)
}

/**
 * Resultado del análisis de brechas: tus habilidades contra las del mercado, y qué cursos
 * te ayudarían a cerrar la diferencia.
 *
 * Campos iguales a SkillsGapAnalysis en docs/contract/models.ts.
 */
data class SkillsGapAnalysis(
    val targetJobTitle: String,
    val skills: List<SkillGapItem>,
    val recommendations: List<CourseRecommendation>,
) {
    /** Habilidades con brecha, de la más grande a la más chica. */
    val skillsToImprove: List<SkillGapItem> = skills.filter { it.gap > 0 }.sortedByDescending { it.gap }
}

/** Curso sugerido para cerrar una brecha. */
data class CourseRecommendation(
    val id: String,
    val title: String,
    val platform: String,
    val url: String,
    val isPaid: Boolean,
    val duration: String?,
)
