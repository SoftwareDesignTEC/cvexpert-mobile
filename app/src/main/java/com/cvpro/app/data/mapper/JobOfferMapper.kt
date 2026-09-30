package com.cvpro.app.data.mapper

import com.cvpro.app.data.remote.dto.JobOfferDto
import com.cvpro.app.domain.model.JobModality
import com.cvpro.app.domain.model.JobOffer
import com.cvpro.app.domain.model.Salary

fun JobOfferDto.toDomain(): JobOffer {
    return JobOffer(
        id = id,
        title = title,
        company = company,
        companyLogoUrl = companyLogoUrl,
        location = location,
        modality = modality.toJobModality(),
        sector = sector,
        type = type,
        description = description,
        requirements = requirements,
        salary = salary?.let { Salary(min = it.min, max = it.max, currency = it.currency) },
        matchScore = matchScore,
        // El servicio manda ISO 8601; la pantalla muestra la fecha ya legible.
        postedAt = postedAt.toDisplayDate(),
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