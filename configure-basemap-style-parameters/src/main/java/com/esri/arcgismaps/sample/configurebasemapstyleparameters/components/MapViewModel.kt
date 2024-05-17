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

package com.esri.arcgismaps.sample.configurebasemapstyleparameters.components

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.Basemap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.BasemapStyleLanguageStrategy
import com.arcgismaps.mapping.BasemapStyleParameters
import com.arcgismaps.mapping.Viewpoint
import java.util.Locale


class MapViewModel(application: Application) : AndroidViewModel(application) {

    val map = ArcGISMap(BasemapStyle.OsmLightGrayBase).apply {
        initialViewpoint = Viewpoint(
            center = Point(3144804.0, 4904598.0),
            scale = 1e7
        )
    }

    // a list of language strategies options
    val languageStrategyOptions = listOf("Global", "Local")
    // holds current state of strategy chosen by the user, initialised to "Local"
    var chosenStrategy = mutableStateOf(languageStrategyOptions[1])
    // a set of some sample language options
    val specificLanguageOptions = listOf("None", "Bulgarian", "Greek", "Turkish")
    // holds current state of language chosen by the user
    var chosenLanguage = mutableStateOf(specificLanguageOptions[0])

    init {
        // start the app with a local basemap style strategy
        setNewBasemap(chosenStrategy.value)
    }

    /**
     * Basemap is immutable so we need to create a new one to set new parameters. Uses an
     * OpenStreetMap basemap style, because they support localization.
     */
    fun setNewBasemap(languageString: String) {
        val basemapStyleParameters = BasemapStyleParameters().apply {
            languageStrategy = when (languageString) {
                "Global" -> {
                    // SpecificLanguage always overrides the LanguageStrategy, which means the
                    // specific language needs to be set to an empty string in order to use the
                    // language strategy.
                    BasemapStyleLanguageStrategy.Specific(Locale.forLanguageTag(""))
                    BasemapStyleLanguageStrategy.Global
                }

                "Local" -> {
                    // SpecificLanguage always overrides the LanguageStrategy, which means the
                    // specific language needs to be set to an empty string in order to use the
                    // language strategy.
                    BasemapStyleLanguageStrategy.Specific(Locale.forLanguageTag(""))
                    BasemapStyleLanguageStrategy.Local
                }

                "None" -> BasemapStyleLanguageStrategy.Specific(Locale.forLanguageTag(""))
                "Bulgarian" -> BasemapStyleLanguageStrategy.Specific(Locale.forLanguageTag("bg"))
                "Greek" -> BasemapStyleLanguageStrategy.Specific(Locale.forLanguageTag("el"))
                "Turkish" -> BasemapStyleLanguageStrategy.Specific(Locale.forLanguageTag("tr"))
                else -> BasemapStyleLanguageStrategy.Specific(Locale.forLanguageTag(""))
            }
        }
        // set a new basemap with the chosen style parameters
        map.setBasemap(Basemap(BasemapStyle.OsmLightGray, basemapStyleParameters))
    }
}
