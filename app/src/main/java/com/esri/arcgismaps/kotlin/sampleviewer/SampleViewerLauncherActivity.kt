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
        startMainActivity()
    }

    private fun startMainActivity(){
        startActivity(Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        Log.e("ERROR MESSAGE", throwable.message.toString())
        throwable.printStackTrace()
        startMainActivity()
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