package com.cvpro.app.data.repository

import android.content.Context
import com.cvpro.app.data.remote.api.CvApi
import com.cvpro.app.data.mapper.toDomain
import com.cvpro.app.data.remote.dto.ExpandExperienceRequestDto
import com.cvpro.app.data.remote.dto.GenerateCvRequestDto
import com.cvpro.app.data.remote.dto.GenerateTextRequestDto
import com.cvpro.app.data.remote.dto.TranslateExperienceRequestDto
import com.cvpro.app.domain.model.AiTextField
import com.cvpro.app.domain.model.Cv
import com.cvpro.app.domain.model.NonLaborExperience
import com.cvpro.app.domain.model.TranslatedExperience
import com.cvpro.app.domain.model.WorkExperience
import com.cvpro.app.domain.repository.CvAiRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiCvAiRepository @Inject constructor(
    private val api: CvApi,
    @param:ApplicationContext private val context: Context,
) : CvAiRepository {

    override suspend fun expandExperience(experience: WorkExperience): String {
        val response = api.expandExperience(
            ExpandExperienceRequestDto(
                company = experience.company.trim(),
                position = experience.position.trim(),
                description = experience.description.trim(),
                // Los CV de la Fase I no traían tipo; el contrato lo exige, así que se asume
                // empleo formal, que es lo mismo que hace el editor con una experiencia nueva.
                type = experience.type.ifBlank { "formal" },
                language = currentLanguage(),
            ),
        )
        // Una respuesta sin texto no debe borrar lo que el usuario ya había escrito.
        return response.description?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("La IA respondió sin descripción")
    }

    override suspend fun translateExperience(experience: NonLaborExperience): TranslatedExperience {
        val response = api.translateExperience(
            TranslateExperienceRequestDto(
                type = experience.type.apiValue,
                position = experience.position.trim(),
                organization = experience.organization.trim().ifBlank { null },
                description = experience.description.trim(),
                language = currentLanguage(),
            ),
        )
        val description = response.description?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("La IA respondió sin descripción")
        return TranslatedExperience(
            // Si la IA no propone puesto, queda el que escribió el usuario.
            position = response.position?.takeIf { it.isNotBlank() } ?: experience.position.trim(),
            description = description,
        )
    }

    override suspend fun generateText(field: AiTextField, context: String): String {
        val response = api.generateText(
            GenerateTextRequestDto(field = field.apiValue, context = context.trim(), language = currentLanguage()),
        )
        return response.text?.takeIf { it.isNotBlank() }?.trim()
            ?: throw IllegalStateException("La IA respondió sin texto")
    }

    override suspend fun generateCv(
        title: String,
        templateId: String,
        fullName: String,
        phone: String?,
        freeText: String,
    ): Cv = api.generateCv(
        GenerateCvRequestDto(
            title = title,
            templateId = templateId,
            fullName = fullName.trim(),
            phone = phone?.trim()?.ifBlank { null },
            freeText = freeText.trim(),
            language = currentLanguage(),
        ),
    ).toDomain()

    /**
     * El idioma de la app ("es" o "en"), para que la IA escriba en el mismo idioma que el
     * resto del CV. Es la misma regla que usa el registro en Notificaciones.
     */
    private fun currentLanguage(): String {
        val language = context.resources.configuration.locales[0].language
        return if (language == "en") "en" else "es"
    }
}
