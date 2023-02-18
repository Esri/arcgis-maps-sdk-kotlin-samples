package com.esri.arcgismaps.sample.generateofflinemapusingworkmanager

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import kotlinx.coroutines.delay

class JobWorker(private val context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    // need to override this for < android 12 platforms
    override suspend fun getForegroundInfo(): ForegroundInfo {
        return super.getForegroundInfo()
    }

    override suspend fun doWork(): Result {
        delay(10000)
        return Result.success()
    }
}