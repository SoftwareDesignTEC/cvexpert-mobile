package com.cvpro.app.domain.repository

import com.cvpro.app.domain.model.JobModality
import com.cvpro.app.domain.model.UserProfile

interface ProfileRepository {

    suspend fun getProfile(
        id: String,
    ): UserProfile

    /**
     * Guarda el perfil. El PUT del servicio de Usuarios es parcial: lo que va en null no se
     * toca. Por eso [bio], [types] y [modalities] son opcionales: null deja lo que había (por
     * ejemplo, lo que se guardó desde la web).
     *
     * @param types jornadas con los valores del contrato ("full-time", "part-time"...).
     */
    suspend fun updateProfile(
        id: String,
        name: String,
        email: String,
        phone: String?,
        location: String,
        bio: String? = null,
        types: List<String>? = null,
        modalities: List<JobModality>? = null,
    ): UserProfile
}
