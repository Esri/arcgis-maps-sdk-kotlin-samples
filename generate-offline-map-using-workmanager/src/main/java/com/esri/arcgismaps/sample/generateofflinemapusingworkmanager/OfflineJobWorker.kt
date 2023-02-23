package com.esri.arcgismaps.sample.generateofflinemapusingworkmanager

import android.app.ForegroundServiceStartNotAllowedException
import android.content.Context
import android.util.Log
import androidx.work.*
import com.arcgismaps.tasks.Job
import com.arcgismaps.tasks.JobStatus
import kotlinx.coroutines.*
import java.io.File
import java.io.FileNotFoundException

class OfflineJobWorker(private val context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    private val TAG = OfflineJobWorker::class.java.simpleName

    private val notificationId by lazy {
        inputData.getInt(notificationIdParameter, 1)
    }

    private val workerNotification by lazy {
        WorkerNotification(applicationContext, notificationId)
    }

    private fun createForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(notificationId, workerNotification.createProgressNotification())
    }

    override suspend fun doWork(): Result {

        val deferred = CompletableDeferred<Result>()
        val coroutineScope = CoroutineScope(Dispatchers.Default)
        val jobJsonFilePath = inputData.getString(jobParameter) ?: return Result.failure()

        return try {
            setForeground(createForegroundInfo())

            val jobJsonFile = File(jobJsonFilePath)
            val jobJson = jobJsonFile.readText()
            val job = Job.fromJson(jobJson) ?: return Result.failure()

            coroutineScope.launch {
                job.progress.collect { progress ->
                    ensureActive()
                    setProgress(workDataOf(Pair("Progress", progress)))
                    Log.d(TAG, "doWork: $progress ${job.status.value}")
                    workerNotification.updateProgressNotification(progress)
                }
            }

            coroutineScope.launch {
                job.messages.collect {
                    Log.d(TAG, "doWork message update: ${it.message}")
                }
            }

            coroutineScope.launch {
                job.status.collect { jobStatus ->
                    when (jobStatus) {
                        JobStatus.Succeeded -> {
                            workerNotification.showStatusNotification("Completed")
                            deferred.complete(Result.success())
                        }
                        JobStatus.Failed -> {
                            workerNotification.showStatusNotification("Failed")
                            deferred.complete(Result.failure())
                            cancel()
                        }
                        else -> {}
                    }
                }
            }

            // runs in dispatchers.io so dowork can run in dispatchers.default
            job.start()
            val result = deferred.await()
            Log.d(TAG, "doWork done: $result")
            // cancel the coroutines and the emitters
            coroutineScope.cancel()
            jobJsonFile.deleteRecursively()
            result
        }  catch (exception: Exception) {
            Log.e(TAG, "Offline map job failed:", exception)
            coroutineScope.cancel()
            workerNotification.showStatusNotification("Failed")
            Result.failure()
        }
    }
}
