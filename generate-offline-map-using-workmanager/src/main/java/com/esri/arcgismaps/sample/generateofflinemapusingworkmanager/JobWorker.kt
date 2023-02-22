package com.esri.arcgismaps.sample.generateofflinemapusingworkmanager

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.arcgismaps.tasks.Job
import kotlinx.coroutines.*
import java.io.File

class JobWorker(private val context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private val TAG = MainActivity::class.java.simpleName

    private fun createForegroundInfo(progress: String): ForegroundInfo {

        val notificationId = inputData.getInt(notificationIdParameter, 1)

        // Make a channel if necessary
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        val title = "Downloading offline map"
        val name = "notifications"
        val description = "VERBOSE_NOTIFICATION_CHANNEL_DESCRIPTION"
        val notificationChannel = "${context.packageName}-notifications"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(notificationChannel, name, importance)
        channel.description = description
        channel.setSound(null, null)

        // Add the channel
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?

        notificationManager?.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(applicationContext, notificationChannel)
            .setContentTitle(title)
            .setTicker(title)
            .setContentText(progress)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            // Add the cancel action to the notification which can
            // be used to cancel the worker
            //.addAction(android.R.drawable.ic_delete, cancel, intent)
            .build()

        return ForegroundInfo(notificationId, notification)
    }

    override suspend fun doWork(): Result {
        setForeground(createForegroundInfo("0"))
        val jobJsonFilePath = inputData.getString(jobParameter) ?: return Result.failure()
        Log.d(TAG, "doWork: $jobJsonFilePath")

        val jobJsonFile = File(jobJsonFilePath)
        val jobJson = jobJsonFile.readText()
        val job = Job.fromJson(jobJson) ?: return Result.failure()

//        val deferred = CompletableDeferred<Result>()
//
//        val coroutineScope = CoroutineScope(Dispatchers.Default)

        for (i in 1..100) {
            delay(500)
            Log.d(TAG, "doWork: $i")
            setForeground(createForegroundInfo(i.toString()))
            setProgress(workDataOf(Pair("Progress", i)))
        }

        return Result.success()

//        coroutineScope.launch {
//            job.progress.collect { progress ->
//                Log.d(TAG, "doWork Progress: $progress")
//                setProgress(workDataOf(Pair("Progress", progress)))
//            }
//        }
//
//        coroutineScope.launch {
//            job.messages.collect { msg ->
//                Log.d(TAG, "doWork: ${msg.message}")
//            }
//        }
//
//        coroutineScope.launch {
//            job.status.collect { jobStatus ->
//                when (jobStatus) {
//                    JobStatus.Succeeded -> {
//                        Log.d(TAG, "doWork: Success")
//                        deferred.complete(Result.success())
//                    }
//                    JobStatus.Failed -> {
//                        Log.d(TAG, "doWork: Failed")
//                        deferred.complete(Result.failure())
//                    }
//                    else -> {}
//                }
//            }
//        }
//
//        job.start()
//        val result = deferred.await()
//        coroutineScope.cancel()
//        if (result == Result.success()) {
//            jobJsonFile.deleteRecursively()
//        }
//        return result
    }
}
