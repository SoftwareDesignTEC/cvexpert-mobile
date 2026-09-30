package com.cvpro.app.data.mapper

import com.cvpro.app.data.remote.dto.CourseRecommendationDto
import com.cvpro.app.data.remote.dto.SkillGapItemDto
import com.cvpro.app.data.remote.dto.SkillsGapAnalysisDto
import com.cvpro.app.domain.model.CourseRecommendation
import com.cvpro.app.domain.model.SkillGapItem
import com.cvpro.app.domain.model.SkillsGapAnalysis

/**
 * Pasa el análisis de la API al modelo que usan las pantallas.
 *
 * El dominio no guarda id, userId ni createdAt: la app muestra el análisis del usuario con
 * sesión abierta y no necesita distinguir entre varios.
 */
fun SkillsGapAnalysisDto.toDomain(): SkillsGapAnalysis = SkillsGapAnalysis(
    targetJobTitle = targetJobTitle.orEmpty(),
    // orEmpty en vez de confiar en el JSON: si el mock no manda la lista, la pantalla
    // muestra su estado vacío en vez de caerse.
    skills = skills.orEmpty().map { it.toDomain() },
    recommendations = recommendations.orEmpty().map { it.toDomain() },
)

private fun SkillGapItemDto.toDomain(): SkillGapItem = SkillGapItem(
    name = name.orEmpty(),
    // El servicio manda de 0 a 100; se recorta por si llega algo fuera de rango, que
    // dibujaría el radar fuera de su área.
    userLevel = (userLevel ?: 0).coerceIn(0, 100),
    marketLevel = (marketLevel ?: 0).coerceIn(0, 100),
)

private fun CourseRecommendationDto.toDomain(): CourseRecommendation = CourseRecommendation(
    id = id.orEmpty(),
    title = title.orEmpty(),
    platform = platform.orEmpty(),
    url = url.orEmpty(),
    isPaid = isPaid ?: false,
    duration = duration,
)
