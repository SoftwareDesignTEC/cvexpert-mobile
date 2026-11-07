package com.cvpro.app.data.mapper

import com.cvpro.app.data.remote.dto.CvTemplateDto
import com.cvpro.app.data.remote.dto.TemplateLayoutDto
import com.cvpro.app.domain.model.CvLayout
import com.cvpro.app.domain.model.CvLayoutSection
import com.cvpro.app.domain.model.CvSection
import com.cvpro.app.domain.model.CvTemplate
import com.cvpro.app.domain.model.HeaderField

/**
 * Pasa la plantilla de la API al modelo que usan las pantallas.
 *
 * Los campos que el mock no manda se reemplazan por valores vacíos en vez de hacer caer la
 * pantalla: hoy no llega `description`, y antes eso rompía todo el catálogo.
 */
fun CvTemplateDto.toDomain(): CvTemplate {
    return CvTemplate(
        id = id.orEmpty(),
        name = name.orEmpty(),
        description = description.orEmpty(),
        previewUrl = previewUrl.orEmpty(),
        category = category.orEmpty(),
        isPremium = isPremium ?: false,
        // Sin dato, se asume que la plantilla ya viene con la app: es lo que el usuario
        // puede usar sin depender de una descarga.
        availableOffline = availableOffline ?: true,
        layout = layout?.toDomain(),
    )
}

/**
 * Colores de cada `colorScheme` del contrato, copiados tal cual de docs/openapi.yaml del
 * servicio de CVs para que el PDF de Android y el de la web salgan del mismo color.
 * Orden: acento, texto, secundario, divisores.
 */
private val COLOR_SCHEMES = mapOf(
    "blue" to listOf(0xFF2563EB, 0xFF1F2937, 0xFF6B7280, 0xFFE5E7EB),
    "green" to listOf(0xFF059669, 0xFF1F2937, 0xFF6B7280, 0xFFE5E7EB),
    "purple" to listOf(0xFF7C3AED, 0xFF1F2937, 0xFF6B7280, 0xFFE5E7EB),
    "gray" to listOf(0xFF374151, 0xFF111827, 0xFF6B7280, 0xFFD1D5DB),
)

/**
 * Convierte la definición del servicio en un [CvLayout], o null si no trae nada que la app
 * sepa dibujar (en ese caso el PDF usa el diseño de siempre).
 *
 * Regla acordada con la web: un `type`, `source`, campo o `colorScheme` que no se conoce se
 * ignora en vez de romper. Así Pérez puede agregar valores nuevos sin sacar una versión
 * nueva de la app.
 */
private fun TemplateLayoutDto.toDomain(): CvLayout? {
    val blocks = sections.orEmpty()

    // El contrato pone el header como una sección más; acá se toma el primero y siempre se
    // dibuja arriba, porque el encabezado de un CV no tiene sentido en otro lugar.
    val header = blocks.firstOrNull { it.type == "header" }
        ?.fields.orEmpty()
        .mapNotNull { path -> HeaderField.entries.firstOrNull { it.apiName == path } }
        .distinct()

    val lists = blocks
        .filter { it.type == "list" }
        .mapNotNull { block ->
            val source = CvSection.entries.firstOrNull { it.apiName == block.source }
                ?: return@mapNotNull null
            // Sin itemFields se muestran todos; si ninguno es válido, la sección no muestra
            // nada y se omite.
            val fields = block.itemFields?.filter { it in source.allFields }?.distinct()
                ?: source.allFields
            if (fields.isEmpty()) return@mapNotNull null
            CvLayoutSection(source = source, fields = fields, column = if (block.column == 2) 2 else 1)
        }

    if (header.isEmpty() && lists.isEmpty()) return null

    // Un colorScheme desconocido cae en azul, que es el primero del contrato.
    val colors = COLOR_SCHEMES[colorScheme] ?: COLOR_SCHEMES.getValue("blue")
    return CvLayout(
        accentColor = colors[0].toInt(),
        textColor = colors[1].toInt(),
        mutedColor = colors[2].toInt(),
        dividerColor = colors[3].toInt(),
        columns = if (columns == 2) 2 else 1,
        header = header,
        sections = lists,
    )
}
