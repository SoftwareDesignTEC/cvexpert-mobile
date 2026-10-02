package com.cvpro.app.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Oferta laboral tal como llega desde la API.
 *
 * Dos campos llegan con otro nombre del que fija el contrato de la web: el mock manda
 * `compatibility` donde el contrato dice `matchScore`, y `createdAt` donde dice `postedAt`.
 * En vez de renombrar el modelo —que rompería la paridad con la web de Pérez— se aceptan
 * los dos nombres con `alternate` de Gson: usa el primero que encuentre en el JSON.
 *
 * Así, cuando Cabrera corrija el mock, la app sigue funcionando sin tocar nada.
 * TODO: avisarle a Cabrera y a Pérez de estos dos nombres.
 *
 * Todos los campos son opcionales a propósito: Gson no respeta los tipos no nulos de
 * Kotlin y un campo ausente haría caer la pantalla.
 */
data class JobOfferDto(
    val id: String? = null,
    val title: String? = null,
    val company: String? = null,
    val companyLogoUrl: String? = null,
    val location: String? = null,
    val modality: String? = null,
    val type: String? = null,
    val sector: String? = null,
    val description: String? = null,
    val requirements: List<String>? = null,
    val salary: SalaryDto? = null,

    @SerializedName(value = "matchScore", alternate = ["compatibility"])
    val matchScore: Int? = null,

    @SerializedName(value = "postedAt", alternate = ["createdAt"])
    val postedAt: String? = null,
)

data class SalaryDto(
    val min: Double? = null,
    val max: Double? = null,
    val currency: String? = null,
)
