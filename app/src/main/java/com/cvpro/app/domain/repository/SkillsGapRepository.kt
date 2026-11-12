package com.cvpro.app.domain.repository

import com.cvpro.app.domain.model.SkillInput
import com.cvpro.app.domain.model.SkillsGapAnalysis

interface SkillsGapRepository {
    val enabled: Boolean
    suspend fun getAnalysis(page: Int = 1): SkillsGapAnalysis
    suspend fun replaceInput(skills: List<SkillInput>)
}
