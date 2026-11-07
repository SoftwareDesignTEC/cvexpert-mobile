package com.cvpro.app.domain.repository

import com.cvpro.app.domain.model.SkillInput
import com.cvpro.app.domain.model.SkillsGapAnalysis

interface SkillsGapRepository {
    suspend fun getAnalysis(page: Long = 1): SkillsGapAnalysis
    suspend fun replaceInput(skills: List<SkillInput>)
}
