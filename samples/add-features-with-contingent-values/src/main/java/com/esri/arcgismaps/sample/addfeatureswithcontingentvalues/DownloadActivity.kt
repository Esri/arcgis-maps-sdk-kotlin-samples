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

package com.esri.arcgismaps.sample.addfeatureswithcontingentvalues

import android.content.Intent
import android.os.Bundle
import com.esri.arcgismaps.sample.sampleslib.DownloaderActivity

class DownloadActivity : DownloaderActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        downloadAndStartSample(
            Intent(this, MainActivity::class.java),
            // get the app name of the sample
            getString(R.string.add_features_with_contingent_values_app_name),
            listOf(
                // Geodatabase containing bird nests defined with contingent values
                "https://www.arcgis.com/home/item.html?id=e12b54ea799f4606a2712157cf9f6e41",
                // Vector tile package of the Fillmore area
                "https://www.arcgis.com/home/item.html?id=b5106355f1634b8996e634c04b6a930a"
            )
        )
    }
}
