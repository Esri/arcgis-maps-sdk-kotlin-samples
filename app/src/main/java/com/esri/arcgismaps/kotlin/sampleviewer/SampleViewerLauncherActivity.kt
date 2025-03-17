/* Copyright 2024 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

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

    /**
     *  Prepares extras for the intent with error information if a [throwable] is provided.
     *  It then starts the [MainActivity] by creating an intent with necessary flag and extras.
     */
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
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtras(extras)
        })
    }

    /**
     * Overrides [ExceptionListener] to capture the [throwable]
     * and starts the [MainActivity] with the exception details.
     */
    override fun uncaughtException(throwable: Throwable) {
        Log.e("SampleViewerException", throwable.message.toString(), throwable)
        startMainActivity(throwable)
    }

    /**
     * Posts a task to the main [Looper] to handle uncaught exceptions.
     */
    private fun setupExceptionHandler() {
        Handler(Looper.getMainLooper()).post {
            while (true) {
                try {
                    Looper.loop()
                } catch (e: Throwable) {
                    uncaughtException(e)
                }
            }
        }
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            uncaughtException(e)
        }
    }
}

/**
 * This interface defines a method [uncaughtException] to handle uncaught exceptions.
 */
interface ExceptionListener {
    fun uncaughtException(throwable: Throwable)
}
