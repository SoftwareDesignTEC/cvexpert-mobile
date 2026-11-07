package com.cvpro.app.domain.repository

import com.cvpro.app.domain.model.NonLaborExperience
import com.cvpro.app.domain.model.TranslatedExperience
import com.cvpro.app.domain.model.WorkExperience

/**
 * Ayudas de IA para escribir el currículum. Las dos llamadas son sin estado: devuelven texto
 * nuevo y no guardan nada; el usuario decide si lo deja y lo guarda con el resto del CV.
 *
 * La IA redacta en el idioma de la app.
 */
interface CvAiRepository {

    /** Redacta de forma profesional la descripción de [experience]. Devuelve la descripción nueva. */
    suspend fun expandExperience(experience: WorkExperience): String

    /**
     * "Traductor de experiencia no laboral": pasa un voluntariado, un proyecto escolar o un
     * trabajo informal a lenguaje de CV.
     */
    suspend fun translateExperience(experience: NonLaborExperience): TranslatedExperience
}
