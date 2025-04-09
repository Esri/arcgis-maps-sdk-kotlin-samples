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

package com.esri.arcgismaps.sample.addfeaturelayers

import android.content.Intent
import android.os.Bundle
import com.esri.arcgismaps.sample.sampleslib.DownloaderActivity

class DownloadActivity : DownloaderActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        downloadAndStartSample(
            Intent(this, MainActivity::class.java),
            // get the app name of the sample
            getString(R.string.add_feature_layers_app_name),
            listOf(
                // ArcGIS Portal item containing the .mmpk mobile map package
                "https://www.arcgis.com/home/item.html?id=cb1b20748a9f4d128dad8a87244e3e37",
                // ArcGIS Portal item containing shapefiles of Scottish Wildlife Trust reserve boundaries
                "https://www.arcgis.com/home/item.html?id=15a7cbd3af1e47cfa5d2c6b93dc44fc2",
                // GeoPackage AuroraCO.gpkg with datasets that cover Aurora Colorado
                "https://www.arcgis.com/home/item.html?id=68ec42517cdd439e81b036210483e8e7"
            )
        )
    }
}
