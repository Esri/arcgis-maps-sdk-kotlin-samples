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

/**
 * Registry of all sample scenarios with their associated test steps.
 */
object SampleScenarioRegistry {
    val scenarios: List<SampleScenario> = listOf(
        SampleScenario(
            id = "display-map",
            activityClass = com.esri.arcgismaps.sample.displaymap.MainActivity::class.java,
            steps = listOf(
                ScenarioStep { activity ->
                    awaitSampleIdle()
                    assertHasGeoView()
                }
            )
        ),
        SampleScenario(
            id = "display-composable-mapview",
            activityClass = com.esri.arcgismaps.sample.displaycomposablemapview.MainActivity::class.java,
            steps = listOf(
                ScenarioStep { activity ->
                    awaitSampleIdle()
                    assertHasGeoView()
                }
            )
        ),
        SampleScenario(
            id = "display-scene",
            activityClass = com.esri.arcgismaps.sample.displayscene.MainActivity::class.java,
            steps = listOf(
                ScenarioStep { activity ->
                    awaitSampleIdle()
                    assertHasGeoView()
                }
            )
        ),
        SampleScenario(
            id = "display-local-scene",
            activityClass = com.esri.arcgismaps.sample.displaylocalscene.MainActivity::class.java,
            steps = listOf(
                ScenarioStep { activity ->
                    awaitSampleIdle()
                    assertHasGeoView()
                }
            )
        )
    )
}
