/* Copyright 2025 Esri
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

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Run with: ./gradlew connectedAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class UIAutomatorScreenshotTest {

    private lateinit var device: UiDevice

    @Before
    fun setup() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.pressHome()
        device.waitForIdle()

        // Launch the app
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = context.packageManager.getLaunchIntentForPackage(
            "com.esri.arcgismaps.kotlin.sampleviewer"
        )?.apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }

        context.startActivity(intent)

        // Wait for app to launch
        device.wait(
            Until.hasObject(By.pkg("com.esri.arcgismaps.kotlin.sampleviewer")),
            10000
        )
        device.waitForIdle()
    }


    @Test
    fun testHomeScreenScreenshot() = runBlocking {
        val screenshotFile = getScreenshotFile("HomeScreen").apply {
            parentFile?.mkdirs()
        }
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        assertTrue(device.takeScreenshot(screenshotFile))
    }

    private fun getScreenshotFile(fileName: String): File {
        val picturesDir = android.os.Environment.getExternalStoragePublicDirectory(
            android.os.Environment.DIRECTORY_PICTURES
        )
        val screenshotDir = File(picturesDir, "SampleViewerScreenshots")
        screenshotDir.mkdirs()
        return File(screenshotDir, "${System.currentTimeMillis()}_${fileName}.png")
    }
}
