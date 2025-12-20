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
 */

package com.esri.arcgismaps.kotlin.samples.scenario

import android.app.Activity
import android.app.Instrumentation
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withClassName
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.hamcrest.CoreMatchers.anyOf
import org.hamcrest.CoreMatchers.endsWith

/**
 * Test scope with helper methods for sample scenario tests.
 */
class SampleScenarioTestScope(val activityClass: Class<out Activity>) {

    val instrumentation: Instrumentation by lazy { InstrumentationRegistry.getInstrumentation() }

    val device: UiDevice by lazy(LazyThreadSafetyMode.NONE) { UiDevice.getInstance(instrumentation) }

    /**
     * Waits for the sample activity to be in the foreground and for the UI thread to be idle.
     */
    fun awaitSampleIdle(timeoutMs: Long = 10_000) {
        device.waitForWindowUpdate(activityClass.packageName, timeoutMs)
        instrumentation.waitForIdleSync()
        device.waitForIdle()
    }

    /**
     * Asserts that either a MapView or SceneView is displayed.
     */
    fun assertHasGeoView() {
        onView(
            anyOf(
                withClassName(endsWith("com.arcgismaps.mapping.view.MapView")),
                withClassName(endsWith("com.arcgismaps.mapping.view.SceneView")),
            )
        ).check(matches(isDisplayed()))
    }
}
