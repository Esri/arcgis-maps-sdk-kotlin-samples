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
import kotlinx.serialization.encodeToString
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