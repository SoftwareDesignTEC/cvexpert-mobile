package com.cvpro.app.domain.repository

import com.cvpro.app.domain.model.AiTextField
import com.cvpro.app.domain.model.Cv
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

    /**
     * Escribe desde cero un texto del CV ([field]) con lo que ya se sabe ([context]). No guarda
     * nada: el editor lo muestra y el usuario decide si lo deja.
     */
    suspend fun generateText(field: AiTextField, context: String): String

    /**
     * Modo IA: arma y guarda un CV completo a partir de lo que el usuario contó en [freeText].
     * Devuelve el CV ya creado, listo para abrir en el editor.
     */
    suspend fun generateCv(
        title: String,
        templateId: String,
        fullName: String,
        phone: String?,
        freeText: String,
    ): Cv
}
