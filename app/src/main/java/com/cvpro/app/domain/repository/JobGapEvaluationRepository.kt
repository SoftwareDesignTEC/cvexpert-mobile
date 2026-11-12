package com.cvpro.app.domain.repository

import com.cvpro.app.domain.model.*

interface JobGapEvaluationRepository {
    suspend fun evaluate(cv: Cv, offer: JobOffer): JobGapEvaluation
}
