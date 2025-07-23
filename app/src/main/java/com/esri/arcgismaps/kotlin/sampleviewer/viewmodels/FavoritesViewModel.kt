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
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import com.esri.arcgismaps.kotlin.sampleviewer.model.Sample
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

/**
 * ViewModel to handle list of favorite samples
 */
class FavoritesViewModel(private val application: Application) : AndroidViewModel(application) {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Retrieve list of favorite Samples using Flow to allow UI to update reactively on change
     */
    fun getFavorites(): Flow<List<Sample>> = application.applicationContext.dataStore.data
        .map { preferences ->
            val jsonString = preferences[FAVORITES_KEY] ?: "[]"
            // Convert JSON string into list of Sample objects
            json.decodeFromString(jsonString)
        }

    /**
     * Save list of favorite Samples
     */
    private suspend fun saveFavorite(favorites: List<Sample>) {
        // Convert list of Sample objects into JSON string
        val jsonString = json.encodeToString(favorites)
        application.applicationContext.dataStore.edit { preferences ->
            // Store JSON string to DataStore using FAVORITES KEY
            preferences[FAVORITES_KEY] = jsonString
        }
    }

    /**
     * Add a new favorite sample to the list of favorite samples
     */
    suspend fun addFavorite(favorite: Sample) {
        val currentFavorites = getFavorites().first()
        val updatedFavorites = currentFavorites.toMutableList().apply {
            add(favorite)
        }
        saveFavorite(updatedFavorites)
    }

    /**
     * Remove an existing favorite sample from the list of favorite samples
     */
    suspend fun removeFavorite(favorite: Sample) {
        val currentFavorites = getFavorites().first()
        val updatedFavorites = currentFavorites.toMutableList().apply {
            remove(favorite)
        }
        saveFavorite(updatedFavorites)
    }

    companion object {
        private const val PREFS_NAME = "favorite_samples_pref"
        private val FAVORITES_KEY = stringPreferencesKey("favorite_samples_key")
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = PREFS_NAME)
    }
}