package com.cvpro.app.data.repository

import com.cvpro.app.domain.model.CourseRecommendation
import com.cvpro.app.domain.model.SkillGapItem
import com.cvpro.app.domain.model.SkillsGapAnalysis
import com.cvpro.app.domain.repository.SkillsGapRepository
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Análisis de prueba mientras no existan los mocks del APIM.
 *
 * TODO: reemplazar por la implementación con Retrofit. Esta clase se borra.
 */
@Singleton
class SampleSkillsGapRepository @Inject constructor() : SkillsGapRepository {

    override suspend fun getAnalysis(): SkillsGapAnalysis {
        delay(NETWORK_DELAY_MS)
        return SkillsGapAnalysis(
            targetJobTitle = "Asistente administrativo",
            skills = listOf(
                SkillGapItem("Comunicación", userLevel = 70, marketLevel = 80),
                SkillGapItem("Excel", userLevel = 45, marketLevel = 85),
                SkillGapItem("Inglés", userLevel = 40, marketLevel = 75),
                SkillGapItem("Atención al cliente", userLevel = 80, marketLevel = 70),
                SkillGapItem("Trabajo en equipo", userLevel = 75, marketLevel = 65),
                SkillGapItem("Redacción", userLevel = 55, marketLevel = 60),
                SkillGapItem("Organización", userLevel = 60, marketLevel = 80),
            ),
            recommendations = listOf(
                CourseRecommendation("1", "Excel desde cero", "YouTube", "https://example.com/excel", isPaid = false, duration = "6 horas"),
                CourseRecommendation("2", "Inglés para el trabajo", "Coursera", "https://example.com/ingles", isPaid = true, duration = "4 semanas"),
                CourseRecommendation("3", "Organización y productividad", "Udemy", "https://example.com/organizacion", isPaid = true, duration = "3 horas"),
            ),
        )
    }

    private companion object {
        const val NETWORK_DELAY_MS = 600L
    }
}
