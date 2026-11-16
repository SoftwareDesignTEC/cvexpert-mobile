package com.cvpro.app.data.notifications

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Recibe lo que manda FCM. Lo declara el AndroidManifest; Android lo levanta solo.
 *
 * - [onNewToken]: FCM le dio a este teléfono un token nuevo; se registra en Notificaciones.
 * - [onMessageReceived]: solo se llama con la app abierta (en segundo plano Android dibuja el
 *   aviso sin pasar por acá). Se dibuja igual, para que el usuario lo vea aunque esté usando
 *   la app, y se avisa a la campana para que actualice el número de no leídas.
 */
@AndroidEntryPoint
class CvMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var pushTokenSync: PushTokenSync

    override fun onNewToken(token: String) {
        pushTokenSync.onNewToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val notification = message.notification ?: return
        PushNotifications.show(
            context = this,
            title = notification.title,
            body = notification.body,
            notificationId = message.data[PushNotifications.EXTRA_NOTIFICATION_ID],
        )
        PushEvents.onPushReceived()
    }
}
