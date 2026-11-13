package com.cvpro.app.data.mapper

import com.cvpro.app.data.remote.dto.JobApplicationDto
import com.cvpro.app.domain.model.ApplicationStatus
import com.cvpro.app.domain.model.JobApplication
import com.cvpro.app.domain.model.JobModality

fun JobApplicationDto.toDomain(): JobApplication {
    return JobApplication(
        id = id.orEmpty(),
        jobTitle = jobTitle.orEmpty(),
        company = company.orEmpty(),
        companyLogoUrl = companyLogoUrl,
        location = location.orEmpty(),
        modality = modality.toJobModality(),
        status = status.toApplicationStatus(),
        // Se guarda en epoch y no como texto: la tarjeta del tablero muestra cuántos días
        // pasaron, y para eso hace falta la fecha y no un texto ya formateado.
        appliedAt = appliedAt.toEpochMillis(),
        jobOfferId = jobOfferId.orEmpty(),
    )
}

private fun String?.toJobModality(): JobModality {
    return when (this?.lowercase()) {
        "remote" -> JobModality.REMOTE
        "on-site" -> JobModality.ON_SITE
        "hybrid" -> JobModality.HYBRID
        // Sin dato o valor que no conocemos: se asume presencial en vez de tumbar la
        // pantalla entera por una postulación mal formada.
        else -> JobModality.ON_SITE
    }
}

private fun String?.toApplicationStatus(): ApplicationStatus {
    return when (this?.lowercase()) {
        "interested" -> ApplicationStatus.INTERESTED
        "applied" -> ApplicationStatus.APPLIED
        "in-progress" -> ApplicationStatus.IN_PROGRESS
        "interview" -> ApplicationStatus.INTERVIEW
        "rejected" -> ApplicationStatus.REJECTED
        // Sin dato o estado desconocido: cae en la primera columna del tablero.
        else -> ApplicationStatus.APPLIED
    }
}