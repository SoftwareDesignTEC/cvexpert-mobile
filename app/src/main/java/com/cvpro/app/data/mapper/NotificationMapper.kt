package com.cvpro.app.data.mapper

import com.cvpro.app.data.remote.dto.NotificationDto
import com.cvpro.app.domain.model.AppNotification
import com.cvpro.app.domain.model.NotificationKind

fun NotificationDto.toDomain(): AppNotification {
    return AppNotification(
        id = id.orEmpty(),
        kind = type.toNotificationKind(),
        title = title.orEmpty(),
        body = body.orEmpty(),
        read = read ?: false,
        createdAt = createdAt.toEpochMillis(),
    )
}

/** Los tipos son los de NotificationType en el contrato de Notificaciones. */
private fun String?.toNotificationKind(): NotificationKind = when (this) {
    "application.status_changed" -> NotificationKind.APPLICATION_STATUS
    "job.new" -> NotificationKind.NEW_JOB
    "cv.generated" -> NotificationKind.CV_GENERATED
    "course.recommended" -> NotificationKind.COURSE_RECOMMENDED
    else -> NotificationKind.OTHER
}
