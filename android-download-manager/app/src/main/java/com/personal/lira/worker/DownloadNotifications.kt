package com.personal.lira.worker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo
import com.personal.lira.MainActivity
import com.personal.lira.R
import com.personal.lira.data.DownloadItem

object DownloadNotifications {
    private const val CHANNEL_ID = "lira_downloads"
    private const val CHANNEL_NAME = "Descargas"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Progreso de descargas multimedia"
            }
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    fun foregroundInfo(context: Context, item: DownloadItem, progress: Int): ForegroundInfo {
        val notification = notification(context, item.title, "Descargando · $progress%", progress, false)
        val id = notificationId(item.id)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                id,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            ForegroundInfo(id, notification)
        }
    }

    fun update(context: Context, item: DownloadItem, progress: Int) {
        notify(context, item.id, notification(context, item.title, "Descargando · $progress%", progress, false))
    }

    fun completed(context: Context, item: DownloadItem) {
        notify(context, item.id, notification(context, item.title, "Descarga completada", 100, true))
    }

    fun failed(context: Context, item: DownloadItem, message: String) {
        notify(context, item.id, notification(context, item.title, message, 0, true))
    }

    private fun notification(
        context: Context,
        title: String,
        text: String,
        progress: Int,
        completed: Boolean,
    ): Notification {
        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openApp)
            .setOnlyAlertOnce(true)
            .setOngoing(!completed)
            .setAutoCancel(completed)
            .setProgress(100, progress, !completed && progress == 0)
            .build()
    }

    private fun notify(context: Context, id: String, notification: Notification) {
        try {
            context.getSystemService(NotificationManager::class.java)
                .notify(notificationId(id), notification)
        } catch (_: SecurityException) {
            // Downloads continue even when the user has denied notification permission.
        }
    }

    private fun notificationId(id: String): Int = (id.hashCode() and 0x7fffffff).coerceAtLeast(1)
}
