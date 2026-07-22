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

package com.esri.arcgismaps.kotlin.samples

import android.app.Activity
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import com.esri.arcgismaps.kotlin.samples.scenario.SampleScenario
import com.esri.arcgismaps.kotlin.samples.scenario.SampleScenarioRegistry
import com.esri.arcgismaps.kotlin.samples.scenario.SampleScenarioTestScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.time.Duration.Companion.milliseconds

/**
 * Parameterized test to run all [SampleScenario]s in the [SampleScenarioRegistry].
 */
@RunWith(Parameterized::class)
class SampleScenarioTests(private val scenario: SampleScenario) {

    /**
     * Provides all sample scenarios for parameterized testing.
     */
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun parameters(): List<SampleScenario> {
            return SampleScenarioRegistry.scenarios
        }
    }

    /**
     * Given a [SampleScenario],
     * When the sample is launched and each step action is performed,
     * Then no exceptions or assertion failures occur.
     */
    @Test
    fun testSampleScenario() = runBlocking {
        val sampleScope = SampleScenarioTestScope(scenario.activityClass)
        val intent = Intent(
            sampleScope.instrumentation.targetContext,
            scenario.activityClass
        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        ActivityScenario.launch<Activity>(intent).use { sampleScenario ->
            for (step in scenario.steps) {
                withTimeout(scenario.timeoutMs.milliseconds) {
                    lateinit var activity: Activity
                    sampleScenario.onActivity { activity = it }
                    step.action(sampleScope, activity)
                }
            }
        }
    }
}
