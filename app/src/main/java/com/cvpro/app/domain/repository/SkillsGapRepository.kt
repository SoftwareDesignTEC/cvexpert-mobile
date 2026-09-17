package com.cvpro.app.domain.repository

import com.cvpro.app.domain.model.SkillsGapAnalysis

/**
 * Análisis de brechas de habilidades.
 *
 * Lo calcula el servicio comparando el perfil del usuario contra las ofertas del puesto al
 * que apunta; la app solo lo muestra.
 */
interface SkillsGapRepository {
    suspend fun getAnalysis(): SkillsGapAnalysis
}
