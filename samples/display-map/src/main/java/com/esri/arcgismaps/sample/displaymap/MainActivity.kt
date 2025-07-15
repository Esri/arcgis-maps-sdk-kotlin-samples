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

package com.esri.arcgismaps.sample.displaymap

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.esri.arcgismaps.sample.displaymap.databinding.DisplayMapActivityMainBinding

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.ACCESS_TOKEN)

        // set up data binding for the activity
        val activityMainBinding: DisplayMapActivityMainBinding =
            DataBindingUtil.setContentView(this, R.layout.display_map_activity_main)
        lifecycle.addObserver(activityMainBinding.mapView)

        // create and add a map with a navigation night basemap style
        val map = ArcGISMap(BasemapStyle.ArcGISNavigationNight)
        activityMainBinding.mapView.map = map
    }
}
