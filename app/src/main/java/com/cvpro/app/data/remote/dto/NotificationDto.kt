package com.cvpro.app.data.remote.dto

/**
 * Cuerpo de `PUT /notifications/directory/me`.
 *
 * @property language "es" o "en".
 * TODO: confirmar con Samuel que espera el código de dos letras y no "es-CR".
 */
data class DirectoryEntryRequestDto(
    val language: String,
)
