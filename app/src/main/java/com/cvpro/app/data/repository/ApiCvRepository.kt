package com.cvpro.app.data.repository

import com.cvpro.app.data.mapper.newCvRequest
import com.cvpro.app.data.mapper.toDomain
import com.cvpro.app.data.mapper.toUpdateRequest
import com.cvpro.app.data.remote.api.CvApi
import com.cvpro.app.data.remote.dto.CvDto
import com.cvpro.app.domain.model.Cv
import com.cvpro.app.domain.model.PagedResult
import com.cvpro.app.domain.model.UserProfile
import com.cvpro.app.domain.repository.CvRepository
import com.cvpro.app.domain.repository.UnsafeCvUpdateException
import com.google.gson.Gson
import com.google.gson.JsonElement
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

@Singleton
class ApiCvRepository @Inject constructor(
    private val api: CvApi,
) : CvRepository {

    override suspend fun getCvs(
        page: Int,
        pageSize: Int,
        query: String,
    ): PagedResult<Cv> {

        val response = api.getCvs(
            page = page,
            pageSize = pageSize,
            search = query.trim().takeIf { it.isNotEmpty() },
        )

        return PagedResult(
            items = response.items.map { it.toDomain() },
            page = response.page,
            pageSize = response.pageSize,
            totalRecords = response.totalRecords,
        )
    }

    override suspend fun getCvById(id: String): Cv {
        return api.getCvById(id).toDomain()
    }

    override suspend fun getCvForSkillUpdate(id: String): Cv {
        val raw = api.getCvForSkillUpdate(id)
        val gson = Gson()
        val cv = gson.fromJson(raw, CvDto::class.java).toDomain()
        val editable = raw.deepCopy().apply {
            // Server-managed metadata is not part of PUT /cvs/{id}.
            listOf("id", "userId", "createdAt", "updatedAt", "previewUrl", "templatePreviewUrl",
                "thumbnailUrl", "imageUrl").forEach { remove(it) }
        }
        if (cv.id != id || !preservesCvFields(editable, gson.toJsonTree(cv.toUpdateRequest())))
            throw UnsafeCvUpdateException()
        return cv
    }

    /**
     * Manda el currículum completo con PUT.
     *
     * El mock devuelve lo que acaba de recibir, así que la pantalla se queda con la
     * respuesta y no con su copia local: si el día de mañana el servicio real cambia algo
     * al guardar (una fecha de modificación, por ejemplo), se ve sin tocar la app.
     */
    override suspend fun updateCv(cv: Cv): Cv {
        return api.updateCv(id = cv.id, request = cv.toUpdateRequest()).toDomain()
    }

    override suspend fun createCv(
        title: String,
        templateId: String,
        personalInfo: UserProfile?,
    ): Cv {
        return api.createCv(
            newCvRequest(
                title = title,
                templateId = templateId,
                personalInfo = personalInfo,
            ),
        ).toDomain()
    }

    override suspend fun deleteCv(id: String) {
        api.deleteCv(id)
    }

    override suspend fun uploadPhoto(jpeg: ByteArray): String {
        // El campo se llama "file", como lo pide el servicio; el nombre del archivo da igual.
        val part = MultipartBody.Part.createFormData(
            name = "file",
            filename = "foto.jpg",
            body = jpeg.toRequestBody("image/jpeg".toMediaType()),
        )
        return api.uploadPhoto(part).url.orEmpty().ifBlank { error("El servicio no devolvió el enlace de la foto") }
    }
}

/** Reject unknown or lossy fields, including nested profile fields and unparseable dates. */
internal fun preservesCvFields(original: JsonElement, replacement: JsonElement?): Boolean = when {
    replacement == null -> original.isJsonNull
    original.isJsonObject && replacement.isJsonObject -> original.asJsonObject.entrySet().all { (key, value) ->
        preservesCvFields(value, replacement.asJsonObject.get(key))
    }
    original.isJsonArray && replacement.isJsonArray -> original.asJsonArray.size() == replacement.asJsonArray.size() &&
        original.asJsonArray.zip(replacement.asJsonArray).all { (before, after) -> preservesCvFields(before, after) }
    else -> original == replacement
}
