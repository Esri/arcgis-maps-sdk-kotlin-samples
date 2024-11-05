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

package com.esri.arcgismaps.sample.displaymapfrommobilemappackage

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.mapping.MobileMapPackage
import com.esri.arcgismaps.sample.displaymapfrommobilemappackage.databinding.DisplayMapFromMobileMapPackageActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : AppCompatActivity() {

    private val provisionPath: String by lazy {
        getExternalFilesDir(null)?.path.toString() + File.separator + getString(R.string.display_map_from_mobile_map_package_app_name)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.ACCESS_TOKEN)

        // set up data binding for the activity
        val activityMainBinding: DisplayMapFromMobileMapPackageActivityMainBinding =
            DataBindingUtil.setContentView(this, R.layout.display_map_from_mobile_map_package_activity_main)
        // create and add the MapView to the lifecycle
        val mapView = activityMainBinding.mapView
        lifecycle.addObserver(mapView)

        // get the file path of the (.mmpk) file
        val filePath = provisionPath + getString(R.string.yellowstone_mmpk)

        // create the mobile map package
        val mapPackage = MobileMapPackage(filePath)

        lifecycleScope.launch {
            // load the mobile map package
            mapPackage.load().getOrElse {
                showError(it.message.toString(), mapView)
            }
            // add the map from the mobile map package to the MapView
            mapView.map = mapPackage.maps.first()
        }
    }

    private fun showError(message: String, view: View) {
        Log.e(localClassName, message)
        Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show()
    }
}
