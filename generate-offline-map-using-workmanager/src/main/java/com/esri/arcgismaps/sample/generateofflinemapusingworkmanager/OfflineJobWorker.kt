package com.esri.arcgismaps.sample.generateofflinemapusingworkmanager

import android.content.Context
import android.util.Log
import androidx.work.*
import com.arcgismaps.tasks.JobStatus
import com.arcgismaps.tasks.offlinemaptask.GenerateOfflineMapJob
import kotlinx.coroutines.*
import java.io.File

/**
 * Class to run a GenerateOfflineMapJob as a CoroutineWorker using WorkManager.
 */
class OfflineJobWorker(private val context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    private val TAG = OfflineJobWorker::class.java.simpleName

    // notificationId passed by the activity
    private val notificationId by lazy {
        inputData.getInt(notificationIdParameter, 1)
    }

    // WorkerNotification instance
    private val workerNotification by lazy {
        WorkerNotification(context, notificationId)
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        // create and return a new ForegroundInfo using the notificationId and
        // a new progress notification
        return ForegroundInfo(notificationId, workerNotification.createProgressNotification())
    }

    override suspend fun doWork(): Result {
        // CoroutineScope to run the GenerateOfflineMapJob's state flows on
        val coroutineScope = CoroutineScope(Dispatchers.Default)

        // get the job parameter which is the json file path
        val offlineJobJsonPath = inputData.getString(jobParameter) ?: return Result.failure()
        // load the json file
        val offlineJobJsonFile = File(offlineJobJsonPath)
        // if the file doesn't exist return failure
        if (!offlineJobJsonFile.exists()) {
            return Result.failure()
        }

        // create the GenerateOfflineMapJob from the json file
        val generateOfflineMapJob =
            GenerateOfflineMapJob.fromJson(offlineJobJsonFile.readText())
            // return failure if the created job is null
                ?: return Result.failure()

        return try {
            // set this worker to run as a long-running foreground service
            // this will throw an exception, if the worker is launched when the app is
            // not in foreground
            setForeground(getForegroundInfo())

            // A deferred to wait for the completion of the generateOfflineMapJob
            val deferred = CompletableDeferred<Result>()
            // check and delete if the offline map package file already exists
            // this check is needed, if the download has failed midway and is restarted later
            // WorkManager
            File(generateOfflineMapJob.downloadDirectoryPath).deleteRecursively()

            // launch the job progress collector
            coroutineScope.launch {
                generateOfflineMapJob.progress.collect { progress ->
                    // update the worker progress
                    setProgress(workDataOf("Progress" to progress))
                    // update the progress notification
                    workerNotification.updateProgressNotification(progress)
                }
            }

            // launch the job status collector
            coroutineScope.launch {
                generateOfflineMapJob.status.collect { jobStatus ->
                    if (jobStatus == JobStatus.Succeeded) {
                        // if the job is successful show a final status notification
                        workerNotification.showStatusNotification("Completed")
                        // complete the deferred with a success result
                        deferred.complete(Result.success())
                    } else if (jobStatus == JobStatus.Failed) {
                        // if the job failed show a final status notification
                        workerNotification.showStatusNotification("Failed")
                        // complete the deferred with a failure result
                        deferred.complete(Result.failure())
                    }
                }
            }

            // start the generateOfflineMapJob
            // this job internally runs on a Dispatchers.IO context, hence this CoroutineWorker
            // can be run on the default Dispatchers.Default context
            generateOfflineMapJob.start()
            // wait for the completion of the deferred value and return its result
            deferred.await()
        } catch (exception: Exception) {
            // capture and log if exception occurs
            Log.e(TAG, "Offline map job failed:", exception)
            if (exception !is CancellationException) {
                // post a job failed notification only when it is not cancelled
                workerNotification.showStatusNotification("Failed")
            }
            // return a failure result
            Result.failure()
        } finally {
            // cancel the created coroutineScope that cancels all of the
            // subscribed state flows
            coroutineScope.cancel()
            // cancel the job
            generateOfflineMapJob.cancel()
        }
    }
}
