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

/**
 * Contract for one sample launch + UI interactions / assertions.
 */
data class SampleScenario(
    val id: String,
    val activityClass: Class<out Activity>,
    val timeoutMs: Long = 10_000,
    val steps: List<ScenarioStep>
) {
    /**
     * Returns the scenario ID for display in test runner UIs.
     */
    override fun toString(): String = id
}

/**
 * A single step in a [SampleScenario] consisting of an action to be performed on the sample UI.
 */
data class ScenarioStep(
    val action: suspend SampleScenarioTestScope.(Activity) -> Unit
)
