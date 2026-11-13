package com.cvpro.app.data.remote.dto

/** Respuesta de POST /jobs/refresh: cuántas ofertas entraron en la búsqueda. */
data class RefreshJobsDto(
    val created: Int = 0,
    val updated: Int = 0,
    val failedQueries: Int = 0,
)
