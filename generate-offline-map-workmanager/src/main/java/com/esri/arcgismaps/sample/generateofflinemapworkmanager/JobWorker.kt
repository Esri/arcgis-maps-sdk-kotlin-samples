/*
 * COPYRIGHT 1995-2022 ESRI
 *
 * TRADE SECRETS: ESRI PROPRIETARY AND CONFIDENTIAL
 * Unpublished material - all rights reserved under the
 * Copyright Laws of the United States.
 *
 * For additional information, contact:
 * Environmental Systems Research Institute, Inc.
 * Attn: Contracts Dept
 * 380 New York Street
 * Redlands, California, USA 92373
 *
 * email: contracts@esri.com
 */

package com.esri.arcgismaps.sample.generateofflinemapworkmanager

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.getChannelId
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.arcgismaps.tasks.Job
import com.arcgismaps.tasks.JobStatus
import kotlinx.coroutines.*

const val notificationIdParameter = "NotificationId"
const val jobParameter = "Job"
const val progressKey = "Progress"

/**
 *
 *
 */
class JobWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notificationId = inputData.getInt(notificationIdParameter, 1)
        val builder = NotificationCompat.Builder(context, "${context.packageName}-notifications")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("generate-offline-map")
            .setContentText("")
//            .setContentIntent(contentPendingIntent)
            .setOngoing(true)
            .setAutoCancel(true)

        return ForegroundInfo(notificationId, builder.build())
    }

    override suspend fun doWork(): Result {
        setForeground(getForegroundInfo())

        val jobJson = inputData.getString(jobParameter) ?: return Result.failure()
        val runtimeJob = Job.fromJson(jobJson) ?: return Result.failure()

        val deferred = CompletableDeferred<Result>()

        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            runtimeJob.progress.collect { progress ->
                setProgress(workDataOf(progressKey to progress))
            }
        }

        scope.launch {
            runtimeJob.status.collect { jobStatus ->
                if (jobStatus == JobStatus.Succeeded) {
                    deferred.complete(Result.success())
                } else if (jobStatus == JobStatus.Failed) {
                    deferred.complete(Result.failure())
                }
            }
        }

        runtimeJob.start()

        val result = deferred.await()
        scope.cancel()

        return result
    }
}