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
import androidx.test.platform.app.InstrumentationRegistry
import com.esri.arcgismaps.kotlin.sampleviewer.model.DefaultSampleInfoRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds

/**
 * Tests for the [DefaultSampleInfoRepository] to verify it correctly loads sample info
 * from the generated assets.
 */
class SampleInfoRepositoryTest {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Given the generated assets in 'samples/samples.json',
     * when the DefaultSampleInfoRepository is loaded,
     * then the count of samples emitted by [DefaultSampleInfoRepository.getAllSamples]
     * should match the JSON sample count.
     */
    @Test
    fun verifySampleCount() = runBlocking {
        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

        // Read the generated asset directly and count top-level entries.
        val samplesJsonString = context.assets.open("samples/samples.json").bufferedReader().use { it.readText() }
        val parsed = json.decodeFromString<Map<String, Map<String, String>>>(samplesJsonString)
        val jsonCount = parsed.size

        // Load the repository from the generated assets.
        DefaultSampleInfoRepository.load(context)

        // Wait for the repository flow to be populated.
        // The StateFlow emits an initial empty list, await until it emits the expected count.
        val sampleViewerCount = withTimeout(10_000.milliseconds) {
            DefaultSampleInfoRepository.getAllSamples()
                .first { samples -> samples.size >= jsonCount }
                .size
        }

        // Assert that the counts match.
        assertEquals(jsonCount, sampleViewerCount)
    }
}
