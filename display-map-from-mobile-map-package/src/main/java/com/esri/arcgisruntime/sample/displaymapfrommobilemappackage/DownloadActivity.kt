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

package com.esri.arcgisruntime.sample.displaymapfrommobilemappackage

import android.content.Intent
import android.os.Bundle
import com.esri.arcgisruntime.sample.sampleslib.DownloaderActivity

class DownloadActivity : DownloaderActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        doDownloadThenStartSample(
            Intent(this, MainActivity::class.java),
            // get the file path of the (.mmpk) file
            getExternalFilesDir(null)?.path + getString(R.string.yellowstone_mmpk),
            // ArcGIS Portal item containing the .mmpk mobile map package
            "https://www.arcgis.com/home/item.html?id=e1f3a7254cb845b09450f54937c16061"
        )
    }
}
