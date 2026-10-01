package com.cvpro.app.data.mapper

import com.cvpro.app.data.remote.dto.CvTemplateDto
import com.cvpro.app.domain.model.CvTemplate

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
    )
}
