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

package com.esri.arcgismaps.kotlin.sampleviewer.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.esri.arcgismaps.kotlin.sampleviewer.model.DefaultSampleInfoRepository
import com.esri.arcgismaps.kotlin.sampleviewer.model.Sample
import com.esri.arcgismaps.kotlin.sampleviewer.model.room.AppDatabase
import com.esri.arcgismaps.kotlin.sampleviewer.model.room.OkapiBM25
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Viewmodel to handle search logic such as finding relevant API keywords and samples
 */
class SampleSearchViewModel(private val application: Application) : AndroidViewModel(application) {

    var rankedSearchResults = MutableStateFlow<MutableList<Sample>>(mutableListOf())
        private set

    var searchSuggestions = MutableStateFlow<List<Pair<String, Boolean>>>(emptyList())
        private set

    // Keep track of the scope where DB is queried, so it can be cancelled
    // on subsequent searchQuery.
    private var databaseQueryJob: Job? = null

    /**
     * Used to search through samples and list relevant Samples and API searches
     */
    fun suggestionSearch(searchQuery: String) {
        viewModelScope.launch {
            val database = AppDatabase.getDatabase(application).sampleDao()
            // If the dao is querying, cancel and query on a new scope.
            databaseQueryJob?.cancelAndJoin()
            databaseQueryJob = launch(Dispatchers.IO) {
                val filteredSamples = database.getFilteredSamples(searchQuery)
                val filteredAPIs = database.getFilteredRelevantAPIs(searchQuery).flatMap { sample ->
                    sample.sampleRelevantApi.filter { apiName ->
                        apiName.lowercase().contains(searchQuery.lowercase())
                    }
                }.distinct()
                // Combine filtered samples and APIs into pairs
                searchSuggestions.value = (filteredSamples.map { sample ->
                    // true indicates it's a filtered sample
                    Pair(sample.sampleName, true)
                } + filteredAPIs.map { apiName ->
                    // false indicates it's a filtered API
                    Pair(apiName, false)
                }).sortedBy { it.first }
            }
        }
    }

    /**
     * Performs a ranked search on the given [searchQuery]
     */
    fun rankedSearch(searchQuery: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val database = AppDatabase.getDatabase(application).sampleDao()
            val cursor = database.matchInfoSearch(searchQuery)

            if (cursor.moveToFirst()) {
                val results = mutableListOf<Sample>()
                do {
                    // Get a reference to the mapInfo blob.
                    val matchInfo = cursor.getBlob(4).toIntArray()
                    // Get the score for each column.
                    val totalScore = listOf(
                        OkapiBM25.score(matchInfo, 0),
                        OkapiBM25.score(matchInfo, 1),
                        OkapiBM25.score(matchInfo, 2),
                        OkapiBM25.score(matchInfo, 3)
                    ).average()
                    // Add the score to the sample.
                    val sample = DefaultSampleInfoRepository.getSampleByName(
                        sampleName = cursor.getString(0)
                    ).apply {
                        score = totalScore
                    }
                    results.add(sample)
                } while (!cursor.isClosed && cursor.moveToNext())
                cursor.close()
                results.sortByDescending { it.score }

                // update the ranked list with the sorted results
                if (rankedSearchResults.value != results) {
                    rankedSearchResults.value = results
                }
            }
        }
    }
}

fun ByteArray.toIntArray(): Array<Int> {
    val intBuf = ByteBuffer.wrap(this)
        .order(ByteOrder.LITTLE_ENDIAN)
        .asIntBuffer()
    val array = IntArray(intBuf.remaining())
    intBuf.get(array)
    return array.toTypedArray()
}
