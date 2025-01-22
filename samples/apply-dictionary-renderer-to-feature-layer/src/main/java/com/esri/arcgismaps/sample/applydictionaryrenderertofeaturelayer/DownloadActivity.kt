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

package com.esri.arcgismaps.sample.applydictionaryrenderertofeaturelayer

import android.content.Intent
import android.os.Bundle
import com.esri.arcgismaps.sample.sampleslib.DownloaderActivity

class DownloadActivity : DownloaderActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        downloadAndStartSample(
            Intent(this, MainActivity::class.java),
            // get the app name of the sample
            getString(R.string.apply_dictionary_renderer_to_feature_layer_app_name),
            listOf(
                // A stylx file that incorporates the MIL-STD-2525D symbol dictionary
                "https://www.arcgis.com/home/item.html?id=c78b149a1d52414682c86a5feeb13d30",
                // A mobile geodatabase created from the ArcGIS for Defense Military Overlay template
                "https://www.arcgis.com/home/item.html?id=e0d41b4b409a49a5a7ba11939d8535dc"
            )
        )
    }
}
