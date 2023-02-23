package com.esri.arcgismaps.sample.generateofflinemapusingworkmanager

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import com.arcgismaps.tasks.Job
import com.arcgismaps.tasks.JobStatus
import kotlinx.coroutines.*
import java.io.File

class JobWorker(private val context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    private val TAG = MainActivity::class.java.simpleName

    private val title = "Downloading offline map"

    private val name = "notifications"

    private val description = "VERBOSE_NOTIFICATION_CHANNEL_DESCRIPTION"

    private val importance = NotificationManager.IMPORTANCE_HIGH

    private var progress = 0

    private val notificationChannelId by lazy {
        "${context.packageName}-notifications"
    }

    private val notificationId by lazy {
        inputData.getInt(notificationIdParameter, 1)
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(notificationId, createNotification())
    }

    override suspend fun doWork(): Result {
        createNotificationChannel()
        setForeground(getForegroundInfo())
        val jobJsonFilePath = inputData.getString(jobParameter) ?: return Result.failure()
        Log.d(TAG, "doWork: $jobJsonFilePath")

        val jobJsonFile = File(jobJsonFilePath)
        val jobJson = jobJsonFile.readText()
        val job = Job.fromJson(jobJson) ?: return Result.failure()

        val deferred = CompletableDeferred<Result>()

        val coroutineScope = CoroutineScope(Dispatchers.Default)

//        for (i in 1..100) {
//            delay(500)
//            Log.d(TAG, "doWork: $i")
//            setForeground(createForegroundInfo(i.toString()))
//            setProgress(workDataOf(Pair("Progress", i)))
//        }
//
//        return Result.success()

        coroutineScope.launch {
            job.progress.collect { progress ->
                Log.d(TAG, "doWork Progress: $progress")
                setProgress(workDataOf(Pair("Progress", progress)))
                this@JobWorker.progress = progress
                setForeground(getForegroundInfo())
            }
        }

        coroutineScope.launch {
            job.messages.collect { msg ->
                Log.d(TAG, "doWork: ${msg.message}")
            }
        }

        coroutineScope.launch {
            job.status.collect { jobStatus ->
                when (jobStatus) {
                    JobStatus.Succeeded -> {
                        Log.d(TAG, "doWork: Success")
                        deferred.complete(Result.success())
                    }
                    JobStatus.Failed -> {
                        Log.d(TAG, "doWork: Failed")
                        deferred.complete(Result.failure())
                    }
                    else -> {}
                }
            }
        }

        job.start()
        val result = deferred.await()
        coroutineScope.cancel()
        if (result == Result.success()) {
            jobJsonFile.deleteRecursively()
        }
        return result
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(notificationChannelId, name, importance)
        channel.description = description
        channel.setSound(null, null)

        // Add the channel
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?

        notificationManager?.createNotificationChannel(channel)
    }

    private fun createNotification() : Notification {
        val notification = NotificationCompat.Builder(applicationContext, notificationChannelId)
            .setContentTitle(title)
            .setContentText(progress.toString())
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setOnlyAlertOnce(true)
            // Add the cancel action to the notification which can
            // be used to cancel the worker
            //.addAction(android.R.drawable.ic_delete, cancel, intent)
            .build()
        with(NotificationManagerCompat.from(applicationContext)) {
            notify(notificationId, notification)
        }
        return notification
    }
}
