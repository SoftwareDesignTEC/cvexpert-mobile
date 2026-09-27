package com.cvpro.app.data.mapper

import com.cvpro.app.data.remote.dto.JobOfferDto
import com.cvpro.app.domain.model.JobModality
import com.cvpro.app.domain.model.JobOffer

fun JobOfferDto.toDomain(): JobOffer {
    return JobOffer(
        id = id,
        title = title,
        company = company,
        location = location,
        modality = modality.toJobModality(),
        sector = sector,
        requirements = requirements,
        matchScore = matchScore,
    )
}

private fun String.toJobModality(): JobModality {
    return when (this.lowercase()) {
        "remote" -> JobModality.REMOTE
        "on-site" -> JobModality.ON_SITE
        "hybrid" -> JobModality.HYBRID
        else -> throw IllegalArgumentException("Modalidad desconocida: $this")
    }
}