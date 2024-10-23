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

package com.esri.arcgismaps.sample.addencexchangeset

import android.content.Intent
import android.os.Bundle
import com.esri.arcgismaps.sample.sampleslib.DownloaderActivity

class DownloadActivity : DownloaderActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        downloadAndStartSample(
            Intent(this, MainActivity::class.java),
            // get the app name of the sample
            getString(R.string.add_enc_exchange_set_app_name),
            listOf(
                // ArcGIS Portal item containing ENC hydrography resources
                "https://www.arcgis.com/home/item.html?id=5028bf3513ff4c38b28822d010a4937c",
                // ArcGIS Portal item containing the ENC dataset
                "https://www.arcgis.com/home/item.html?id=9d2987a825c646468b3ce7512fb76e2d"
            )
        )
    }
}
