package com.esri.arcgismaps.sample.generateofflinemapusingworkmanager

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * Helper class that handles progress and status notifications on [applicationContext] for
 * the offline map job run using WorkManager. a non-zero [notificationId] is used to show
 * and update the progress and status notifications
 */
class WorkerNotification(
    private val applicationContext: Context,
    private val notificationId: Int
) {

    // unique channel id for the NotificationChannel
    private val notificationChannelId by lazy {
        "${applicationContext.packageName}-notifications"
    }

    // intent for notifications tap action that launch the MainActivity
    private val mainActivityIntent by lazy {
        // setup the intent to launch MainActivity
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            // launches the activity if not already on top and active
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        // set the pending intent that will be passed to the NotificationManager
        PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
    }

    init {
        // create the notification channel
        createNotificationChannel()
    }

    /**
     * Creates and returns a new progress notification with the given [progress] value
     */
    fun createProgressNotification(progress: Int): Notification {
        // use the default notification builder and set the progress to 0
        return getDefaultNotificationBuilder(
            setOngoing = true,
            contentText = "Download in progress: $progress%"
        ).setProgress(100, progress, false)
            .build()
    }

    /**
     * Creates and posts a new status notification with the [message] and dismisses any ongoing
     * progress notifications
     */
    fun showStatusNotification(message: String) {
        // build using the default notification builder with the status message
        val notification = getDefaultNotificationBuilder(
            setOngoing = false,
            contentText = message
        ).build().apply {
            // this flag dismisses the notification on opening
            flags = Notification.FLAG_AUTO_CANCEL
        }

        with(NotificationManagerCompat.from(applicationContext)) {
            // cancel the visible progress notification using its id
            cancel(notificationId)
            // post the new status notification with a new notificationId
            notify(notificationId + 1, notification)
        }
    }

    /**
     * Creates a new notification channel and adds it to the NotificationManager
     */
    private fun createNotificationChannel() {
        // get the channel properties from resources
        val name = applicationContext.getString(R.string.notification_channel_name)
        val descriptionText =
            applicationContext.getString(R.string.notification_channel_description)
        val importance = NotificationManager.IMPORTANCE_HIGH
        // create a new notification channel with the properties
        val channel = NotificationChannel(notificationChannelId, name, importance).apply {
            description = descriptionText
        }
        // get the notification system service as a NotificationManager
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Add the channel to the NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Creates and returns a new NotificationCompat.Builder with the given [contentText]
     * and as an ongoing notification based on [setOngoing]
     */
    private fun getDefaultNotificationBuilder(
        setOngoing: Boolean,
        contentText: String
    ): NotificationCompat.Builder {
        return NotificationCompat.Builder(applicationContext, notificationChannelId)
            // sets the notifications title
            .setContentTitle(applicationContext.getString(R.string.notification_title))
            // sets the content that is displayed on expanding the notification
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            // sets it to only show the notification alert once, in case of progress
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            // ongoing notifications cannot be dismissed by swiping them away
            .setOngoing(setOngoing)
            // sets the onclick action to launch the mainActivityIntent
            .setContentIntent(mainActivityIntent)
            // sets it to show the notification immediately
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
    }
}
