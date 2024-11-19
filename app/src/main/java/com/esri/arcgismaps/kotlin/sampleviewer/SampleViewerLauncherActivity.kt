package com.esri.arcgismaps.kotlin.sampleviewer

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity

class SampleViewerLauncherActivity : ComponentActivity(), ExceptionListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupExceptionHandler()
        startMainActivity(null)
    }

    private fun startMainActivity(throwable: Throwable?) {
        val extras = Bundle()
        if (throwable != null) {
            extras.putStringArray(
                /* key = */     "SampleViewerException",
                /* value = */   arrayOf(throwable.message.toString(), throwable.cause.toString())
            )
        }

        startActivity(Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtras(extras)
        })
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        Log.e("SampleViewerException", throwable.message.toString(), throwable)
        startMainActivity(throwable)
    }

    private fun setupExceptionHandler() {
        Handler(Looper.getMainLooper()).post {
            while (true) {
                try {
                    Looper.loop()
                } catch (e: Throwable) {
                    uncaughtException(Looper.getMainLooper().thread, e)
                }
            }
        }
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            uncaughtException(t, e)
        }
    }
}


interface ExceptionListener {
    fun uncaughtException(thread: Thread, throwable: Throwable)
}