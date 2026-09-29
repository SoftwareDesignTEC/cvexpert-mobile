package com.cvpro.app.data.mapper

import com.cvpro.app.data.remote.dto.JobApplicationDto
import com.cvpro.app.domain.model.ApplicationStatus
import com.cvpro.app.domain.model.JobApplication
import com.cvpro.app.domain.model.JobModality

fun JobApplicationDto.toDomain(): JobApplication {
    return JobApplication(
        id = id,
        jobTitle = jobTitle,
        company = company,
        companyLogoUrl = companyLogoUrl,
        location = location,
        modality = modality.toJobModality(),
        status = status.toApplicationStatus(),
        // Se guarda tal cual llega, en ISO 8601: la tarjeta del tablero muestra cuántos días
        // pasaron, y para eso hace falta la fecha y no un texto ya formateado.
        appliedAt = appliedAt,
    )
}

private fun String.toJobModality(): JobModality {
    return when (lowercase()) {
        "remote" -> JobModality.REMOTE
        "on-site" -> JobModality.ON_SITE
        "hybrid" -> JobModality.HYBRID
        else -> throw IllegalArgumentException("Modalidad desconocida: $this")
    }
}

private fun String.toApplicationStatus(): ApplicationStatus {
    return when (lowercase()) {
        "applied" -> ApplicationStatus.APPLIED
        "in-progress" -> ApplicationStatus.IN_PROGRESS
        "interview" -> ApplicationStatus.INTERVIEW
        "rejected" -> ApplicationStatus.REJECTED
        else -> throw IllegalArgumentException("Estado de postulación desconocido: $this")
    }
}