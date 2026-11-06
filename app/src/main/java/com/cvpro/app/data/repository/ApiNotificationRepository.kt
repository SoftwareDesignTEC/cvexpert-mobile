package com.cvpro.app.data.repository

import com.cvpro.app.data.mapper.toDomain
import com.cvpro.app.data.remote.api.NotificationApi
import com.cvpro.app.domain.model.AppNotification
import com.cvpro.app.domain.model.PagedResult
import com.cvpro.app.domain.repository.NotificationRepository
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiNotificationRepository @Inject constructor(
    private val api: NotificationApi,
) : NotificationRepository {

    override suspend fun getNotifications(
        page: Int,
        pageSize: Int,
        unreadOnly: Boolean,
    ): PagedResult<AppNotification> {
        // Sin filtro se manda null y Retrofit no agrega el parámetro: el contrato dice que
        // unread=false significa "solo leídas", no "todas".
        val response = api.getNotifications(page, pageSize, unread = if (unreadOnly) true else null)
        return PagedResult(
            items = response.items.map { it.toDomain() },
            page = response.page,
            pageSize = response.pageSize,
            totalRecords = response.totalRecords,
        )
    }

    override suspend fun getUnreadCount(): Int = api.getUnreadCount().count ?: 0

    override suspend fun markRead(id: String) {
        api.markRead(id)
    }

    override suspend fun markAllRead() {
        val response = api.markAllRead()
        // Response<Unit> no lanza en errores HTTP: se lanza acá para que la pantalla lo muestre.
        if (!response.isSuccessful) throw HttpException(response)
    }
}
