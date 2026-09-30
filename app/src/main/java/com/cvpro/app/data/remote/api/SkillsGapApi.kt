package com.cvpro.app.data.remote.api

import com.cvpro.app.data.remote.dto.SkillsGapAnalysisDto
import retrofit2.http.GET

interface SkillsGapApi {

    /**
     * Análisis de brecha del usuario con sesión abierta.
     *
     * Devuelve un solo análisis, no una lista paginada: el servicio ya sabe de quién es por
     * el token que manda el interceptor.
     */
    @GET("skills-gap")
    suspend fun getSkillsGap(): SkillsGapAnalysisDto
}
