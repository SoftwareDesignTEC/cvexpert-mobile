package com.cvpro.app.data.mapper

import com.cvpro.app.data.remote.dto.ActiveSessionDto
import com.cvpro.app.data.remote.dto.LegalDocumentDto
import com.cvpro.app.domain.model.ActiveSession
import com.cvpro.app.domain.model.LegalDocument

fun ActiveSessionDto.toDomain(): ActiveSession =
    ActiveSession(
        id = id,
        deviceName = deviceName,
        platform = platform,
        client = client,
        lastActiveAt = lastActiveAt.toEpochMillis(),
        current = current,
    )

fun LegalDocumentDto.toDomain(): LegalDocument =
    LegalDocument(
        id = id,
        title = title,
        version = version,
        effectiveDate = effectiveDate.toEpochMillis(),
        content = content,
    )