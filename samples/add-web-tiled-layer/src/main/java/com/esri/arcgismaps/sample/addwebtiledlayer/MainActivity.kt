/* Copyright 2023 Esri
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

package com.esri.arcgismaps.sample.addwebtiledlayer

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.Basemap
import com.arcgismaps.mapping.layers.WebTiledLayer
import com.esri.arcgismaps.sample.addwebtiledlayer.databinding.AddWebTiledLayerActivityMainBinding

class MainActivity : AppCompatActivity() {

    // set up data binding for the activity
    private val activityMainBinding: AddWebTiledLayerActivityMainBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.add_web_tiled_layer_activity_main)
    }

    private val mapView by lazy {
        activityMainBinding.mapView
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(mapView)

        // build the web tiled layer from ArcGIS Living Atlas of the World tile service url
        val webTiledLayer = WebTiledLayer.create(
            urlTemplate = getString(R.string.template_uri_living_atlas)
        ).apply {
            // set the attribution on the layer
            attribution = getString(R.string.living_atlas_attribution)
        }

        // use web tiled layer as Basemap
        val map = ArcGISMap(Basemap(webTiledLayer))
        mapView.map = map
    }
}
