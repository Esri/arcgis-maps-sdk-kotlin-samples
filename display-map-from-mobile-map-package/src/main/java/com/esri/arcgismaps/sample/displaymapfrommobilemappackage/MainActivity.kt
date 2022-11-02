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
import com.esri.arcgismaps.sample.displaymapfrommobilemappackage.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.java.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)

        // set up data binding for the activity
        val activityMainBinding: ActivityMainBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_main)
        // create and add the MapView to the lifecycle
        val mapView = activityMainBinding.mapView
        lifecycle.addObserver(mapView)

        // get the file path of the (.mmpk) file
        val filePath = getExternalFilesDir(null)?.path + getString(R.string.yellowstone_mmpk)

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
        Log.e(TAG, message)
        Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show()
    }
}
