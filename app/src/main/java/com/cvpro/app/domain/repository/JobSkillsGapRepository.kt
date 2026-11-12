package com.cvpro.app.domain.repository

import com.cvpro.app.domain.model.Cv
import com.cvpro.app.domain.model.JobOffer
import com.cvpro.app.domain.model.JobSkillsGapAnalysis

interface JobSkillsGapRepository {
    suspend fun analyze(cv: Cv, offer: JobOffer, evaluationOnly: Boolean = false): JobSkillsGapAnalysis
}
