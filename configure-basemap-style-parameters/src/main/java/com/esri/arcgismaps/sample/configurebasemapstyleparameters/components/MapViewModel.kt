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
import androidx.compose.runtime.mutableStateOf
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
    var languageStrategy = mutableStateOf(languageStrategyOptions[1])
    val onLanguageStrategyChange: (String) -> Unit = { languageStrategy.value = it }

    // a set of some sample language options
    val specificLanguageOptions = listOf("None", "Bulgarian", "Greek", "Turkish")
    var specificLanguage = mutableStateOf(specificLanguageOptions[0])
    val onSpecificLanguageChange: (String) -> Unit = { specificLanguage.value = it }

    init {
        // start the app with a local basemap style strategy
        createNewBasemapStyleParameters(languageStrategy.value, specificLanguage.value)
    }

    /**
     * Basemap is immutable so we need to create a new one to set new parameters. Uses an
     * OpenStreetMap basemap style, because they support localization.
     */
    fun createNewBasemapStyleParameters(languageStrategy: String, specificLanguage: String) {
        val basemapStyleParameters = BasemapStyleParameters().apply {
            this.languageStrategy = when (specificLanguage) {
                "None" -> {
                    when (languageStrategy) {
                        "Global" -> BasemapStyleLanguageStrategy.Global
                        "Local" -> BasemapStyleLanguageStrategy.Local
                        else -> { throw(IllegalArgumentException("Invalid language strategy"))}
                    }
                }
                "Bulgarian" -> BasemapStyleLanguageStrategy.Specific(Locale.forLanguageTag("bg"))
                "Greek" -> BasemapStyleLanguageStrategy.Specific(Locale.forLanguageTag("el"))
                "Turkish" -> BasemapStyleLanguageStrategy.Specific(Locale.forLanguageTag("tr"))
                else -> {throw IllegalArgumentException("Invalid language")}
            }
        }
        // set a new basemap with the chosen style parameters
        map.setBasemap(Basemap(BasemapStyle.OsmLightGray, basemapStyleParameters))
    }
}
