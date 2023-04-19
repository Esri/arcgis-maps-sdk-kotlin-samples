package com.esri.arcgismaps.sample.generateofflinemapusingandroidjetpackworkmanager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.WorkManager

/**
 * Custom BroadcastReceiver class that handles notification actions setup by WorkerNotification
 */
class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        // retrieve the data name or return if the context if null
        val extraName = context?.getString(R.string.notification_action) ?: return
        // get the actual data from the intent
        val action = intent?.getStringExtra(extraName) ?: "none"
        // if the action is cancel
        if (action == "Cancel") {
            // get the WorkManager instance and cancel all active workers
            WorkManager.getInstance(context).cancelAllWork()
        }
    }
}
