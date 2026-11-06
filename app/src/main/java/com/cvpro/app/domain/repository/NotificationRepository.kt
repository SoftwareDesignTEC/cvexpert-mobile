package com.cvpro.app.domain.repository

import com.cvpro.app.domain.model.AppNotification
import com.cvpro.app.domain.model.PagedResult

/** Centro de notificaciones del usuario con sesión. El usuario sale del token, no se pasa. */
interface NotificationRepository {

    /** Una página del historial, más recientes primero. Con [unreadOnly] solo las no leídas. */
    suspend fun getNotifications(page: Int, pageSize: Int, unreadOnly: Boolean): PagedResult<AppNotification>

    suspend fun getUnreadCount(): Int

    suspend fun markRead(id: String)

    suspend fun markAllRead()
}
