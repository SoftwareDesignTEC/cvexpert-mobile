package com.cvpro.app.data.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.cvpro.app.MainActivity
import com.cvpro.app.R

/**
 * Los avisos push en la barra del teléfono: el canal y cómo se dibuja cada uno.
 *
 * Con la app cerrada o en segundo plano, Android dibuja el aviso solo con lo que manda FCM
 * (título y texto), en el canal que dice el manifest. Con la app abierta no lo dibuja, así que
 * CvMessagingService llama a [show] para que se vea igual.
 *
 * Al tocarlo se abre la app con el extra `notificationId`, el mismo que FCM pone en los avisos
 * de segundo plano. MainActivity lo ve y abre el centro de notificaciones.
 */
object PushNotifications {

    /** El mismo id que `default_notification_channel_id` en el AndroidManifest. */
    const val CHANNEL_ID = "cvexpert_avisos"

    /** Lo manda Notificaciones en `data` y lo pone Android en el Intent al tocar el aviso. */
    const val EXTRA_NOTIFICATION_ID = "notificationId"

    /**
     * Crea el canal. Desde Android 8 sin canal no se muestra ningún aviso. Crearlo de nuevo no
     * hace nada, así que se llama en cada arranque.
     */
    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.push_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply { description = context.getString(R.string.push_channel_description) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** Dibuja un aviso. Si el usuario no dio permiso, no hace nada. */
    fun show(context: Context, title: String?, body: String?, notificationId: String?) {
        if (!canNotify(context)) return
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_NOTIFICATION_ID, notificationId.orEmpty())
        }
        // Un id por aviso: así dos avisos seguidos no se pisan ni comparten el PendingIntent.
        val id = notificationId?.hashCode() ?: System.currentTimeMillis().toInt()
        val pendingIntent = PendingIntent.getActivity(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setColor(ContextCompat.getColor(context, R.color.brand_blue))
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (e: SecurityException) {
            // El usuario quitó el permiso justo entre la revisión y el aviso.
        }
    }

    /** En Android 13+ hace falta el permiso POST_NOTIFICATIONS; antes, basta con no haberlos apagado. */
    private fun canNotify(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
}
