/* Copyright 2022 Esri
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

package com.esri.arcgismaps.sample.setviewpointrotation

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.esri.arcgismaps.sample.setviewpointrotation.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)

        // set up data binding for the activity
        val activityMainBinding: ActivityMainBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_main)
        val mapView = activityMainBinding.mapView
        val rotationSlider = activityMainBinding.rotationSlider
        val rotationValueText = activityMainBinding.rotationValueText
        lifecycle.addObserver(mapView)

        // create a map with a topographic basemap and initial position
        val map = ArcGISMap(BasemapStyle.ArcGISTopographic)
        // set the map to be displayed in this view
        mapView.map = map
        mapView.setViewpoint(Viewpoint(34.056295, -117.195800, 10000.0))

        rotationSlider.addOnChangeListener { _, angle, _ ->
            // set the text to the value
            rotationValueText.text = angle.toInt().toString()
            // rotate map view to the progress angle
            lifecycleScope.launch {
                mapView.setViewpointRotation(angle.toDouble())
            }
        }
    }
}
