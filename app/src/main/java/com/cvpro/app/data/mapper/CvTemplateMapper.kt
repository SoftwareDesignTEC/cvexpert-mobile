package com.cvpro.app.data.mapper

import com.cvpro.app.data.remote.dto.CvTemplateDto
import com.cvpro.app.domain.model.CvTemplate

fun CvTemplateDto.toDomain(): CvTemplate {
    return CvTemplate(
        id = id,
        name = name,
        description = description,
        previewUrl = previewUrl,
        category = category,
        isPremium = isPremium,
        availableOffline = availableOffline,
    )
}