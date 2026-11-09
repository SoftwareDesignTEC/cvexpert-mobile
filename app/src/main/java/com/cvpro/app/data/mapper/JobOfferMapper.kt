package com.cvpro.app.data.mapper

import com.cvpro.app.data.remote.dto.JobOfferDto
import com.cvpro.app.domain.model.JobModality
import com.cvpro.app.domain.model.JobOffer
import com.cvpro.app.domain.model.Salary

fun JobOfferDto.toDomain(): JobOffer {
    return JobOffer(
        id = id.orEmpty(),
        title = title.orEmpty(),
        company = company.orEmpty(),
        companyLogoUrl = companyLogoUrl,
        location = location.orEmpty(),
        modality = modality.toJobModality(),
        sector = sector.orEmpty(),
        type = type.orEmpty(),
        description = description.orEmpty(),
        requirements = requirements.orEmpty(),
        salary = salary?.let { Salary(min = it.min ?: 0.0, max = it.max ?: 0.0, currency = it.currency.orEmpty()) },
        matchScore = matchScore,
        // Llega en epoch (o ISO en los mocks viejos); la pantalla la muestra ya legible.
        postedAt = postedAt.toEpochMillis().toDisplayDate(),
        applyUrl = applyUrl?.takeIf { it.isNotBlank() },
    )
}

private fun String?.toJobModality(): JobModality {
    return when (this?.lowercase()) {
        "remote" -> JobModality.REMOTE
        "on-site" -> JobModality.ON_SITE
        "hybrid" -> JobModality.HYBRID
        // Sin dato o valor desconocido: se asume presencial.
        else -> JobModality.ON_SITE
    }
}