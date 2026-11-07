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
    // null en las plantillas que todavía no se migraron en Mongo (solo imagen y metadata).
    val layout: TemplateLayoutDto? = null,
)

/**
 * Definición estructurada de la plantilla (`TemplateLayout` en docs/openapi.yaml del servicio
 * de CVs). Los valores llegan como texto del contrato y se validan en el mapper: lo que la app
 * no conoce se ignora, que es lo que se acordó con la web para poder agregar valores después.
 */
data class TemplateLayoutDto(
    val columns: Int? = null,
    val colorScheme: String? = null,
    val sections: List<TemplateSectionDto>? = null,
)

/** Un bloque de la plantilla. `fields` es del header; `source` e `itemFields`, de las listas. */
data class TemplateSectionDto(
    val type: String? = null,
    val column: Int? = null,
    val fields: List<String>? = null,
    val source: String? = null,
    val itemFields: List<String>? = null,
)
