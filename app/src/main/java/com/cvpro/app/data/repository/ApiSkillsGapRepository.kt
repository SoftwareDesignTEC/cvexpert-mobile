package com.cvpro.app.data.repository

import com.cvpro.app.data.mapper.toDomain
import com.cvpro.app.data.remote.api.SkillsGapApi
import com.cvpro.app.domain.model.SkillsGapAnalysis
import com.cvpro.app.domain.repository.SkillsGapRepository
import javax.inject.Inject
import javax.inject.Singleton

/** Análisis de brecha contra los mocks del APIM. */
@Singleton
class ApiSkillsGapRepository @Inject constructor(
    private val api: SkillsGapApi,
) : SkillsGapRepository {

    override suspend fun getAnalysis(): SkillsGapAnalysis = api.getSkillsGap().toDomain()
}
