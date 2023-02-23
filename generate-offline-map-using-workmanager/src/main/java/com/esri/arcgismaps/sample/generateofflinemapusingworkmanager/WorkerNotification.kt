package com.esri.arcgismaps.sample.generateofflinemapusingworkmanager

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class WorkerNotification(
    private val applicationContext: Context,
    private val notificationId: Int
) {

    private val notificationTitle = "Offline Map Download"

    private val name = "notifications"

    private val description = "VERBOSE_NOTIFICATION_CHANNEL_DESCRIPTION"

    private val importance = NotificationManager.IMPORTANCE_HIGH

    private val notificationChannelId by lazy {
        "${applicationContext.packageName}-notifications"
    }

    private val mainActivityIntent by lazy {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        }
        PendingIntent.getActivity(
            applicationContext,
            1,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
    }

    init {
        createNotificationChannel()
    }

    fun createProgressNotification(): Notification {
        return getDefaultNotificationBuilder(
            setOngoing = true,
            contentText = "Download in progress: 0%"
        ).setProgress(100, 0, false)
            .build()

//        return NotificationCompat.Builder(applicationContext, notificationChannelId)
//            .setContentTitle(notificationTitle)
//            .setContentText("Download in progress: 0%")
//            .setProgress(100, 0, false)
//            .setSmallIcon(R.drawable.ic_launcher_foreground)
//            .setOngoing(true)
//            .setOnlyAlertOnce(true)
//            .setContentIntent(mainActivityIntent)
//            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
//            .build().apply {
//                flags = Notification.FLAG_AUTO_CANCEL
//            }
    }

    fun updateProgressNotification(progress: Int) {
        val notification = getDefaultNotificationBuilder(
            setOngoing = true,
            contentText = "Download in progress: $progress%"
        ).setProgress(100, progress, false)
            .build()
//        val notification = NotificationCompat.Builder(applicationContext, notificationChannelId)
//            .setContentTitle(notificationTitle)
//            .setContentText("Download in progress: $progress%")
//            .setProgress(100, progress, false)
//            .setSmallIcon(R.drawable.ic_launcher_foreground)
//            .setOngoing(true)
//            //.setOnlyAlertOnce(true)
//            .setContentIntent(mainActivityIntent)
//            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
//            .build().apply {
//                flags = Notification.FLAG_AUTO_CANCEL
//            }
        with(NotificationManagerCompat.from(applicationContext)) {
            notify(notificationId, notification)
        }
    }

    fun showStatusNotification(message: String) {
        val notification = NotificationCompat.Builder(applicationContext, notificationChannelId)
            .setContentTitle(notificationTitle)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(false)
            .setContentIntent(mainActivityIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build().apply {
                flags = Notification.FLAG_AUTO_CANCEL
            }
        with(NotificationManagerCompat.from(applicationContext)) {
            cancel(notificationId)
            notify(notificationId + 1, notification)
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(notificationChannelId, name, importance)
        channel.description = description

        // Add the channel
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?

        notificationManager?.createNotificationChannel(channel)
    }

    private fun getDefaultNotificationBuilder(
        setOngoing: Boolean,
        contentText: String
    ): NotificationCompat.Builder {
        return NotificationCompat.Builder(applicationContext, notificationChannelId)
            .setContentTitle(notificationTitle)
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setOngoing(setOngoing)
            .setContentIntent(mainActivityIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
    }
}