package com.cvpro.app.data.remote.dto

/**
 * Plantilla tal como llega desde la API.
 *
 * Todos los campos son opcionales a propósito. Gson no respeta los tipos no nulos de
 * Kotlin: si el JSON no trae un campo, lo deja en null igual y la app revienta al armar el
 * modelo, con un error que no dice dónde se originó. Declararlos opcionales hace que la
 * ausencia se resuelva en el mapper, que es donde se puede decidir qué poner.
 */
data class CvTemplateDto(
    val id: String? = null,
    val name: String? = null,
    val description: String? = null,
    val previewUrl: String? = null,
    val category: String? = null,
    val isPremium: Boolean? = null,
    val availableOffline: Boolean? = null,
)
