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

package com.esri.arcgisruntime.sample.maprotation

import android.os.Bundle
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import arcgisruntime.ApiKey
import arcgisruntime.ArcGISRuntimeEnvironment
import arcgisruntime.mapping.ArcGISMap
import arcgisruntime.mapping.BasemapStyle
import arcgisruntime.mapping.Viewpoint
import arcgisruntime.mapping.view.MapView
import com.esri.arcgisruntime.sample.maprotation.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.java.simpleName
    private val scope = CoroutineScope(Dispatchers.Main + CoroutineName(TAG))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISRuntimeEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)

        // set up data binding for the activity
        val activityMainBinding: ActivityMainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        val mapView = activityMainBinding.mapView
        val rotationSeekBar = activityMainBinding.rotationSeekBar
        val rotationValueText = activityMainBinding.rotationValueText
        lifecycle.addObserver(mapView)

        // create a map with a topographic basemap and initial position
        val map = ArcGISMap(BasemapStyle.ArcGISTopographic)
        // set the map to be displayed in this view
        mapView.map = map
        mapView.setViewpoint(Viewpoint(34.056295, -117.195800, 10000.0))

        rotationSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, angle: Int, b: Boolean) {
                // set the text to the value
                rotationValueText.text = angle.toString()
                // rotate map view to the progress angle
                scope.launch {
                    mapView.awaitSetViewpointRotation(angle.toDouble())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }
}
