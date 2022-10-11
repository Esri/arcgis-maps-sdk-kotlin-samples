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
import android.util.Log
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import arcgisruntime.LoadStatus
import com.esri.arcgisruntime.sample.displaymapfrommobilemappackage.databinding.ActivityDownloadBinding
import com.esri.arcgisruntime.sample.sampleslib.SampleActivity
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class DownloadActivity : SampleActivity() {

    // ArcGIS Portal item containing the .mmpk mobile map package
    val provisionURL: String = "https://www.arcgis.com/home/item.html?id=e1f3a7254cb845b09450f54937c16061"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val activityDownloadBinding: ActivityDownloadBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_download)

        // get the file path of the (.mmpk) file
        val filePath = getExternalFilesDir(null)?.path + getString(R.string.yellowstone_mmpk)

        // start the download manager to automatically add the .mmpk file to the app
        // alternatively, you can use ADB/Device File Explorer
        lifecycleScope.launch {
            sampleDownloadManager(provisionURL, filePath).collect { loadStatus ->
                if(loadStatus == LoadStatus.Loaded){
                    // download complete, resuming sample
                    startActivity(Intent(this@DownloadActivity, MainActivity::class.java))
                    finish()
                } else if (loadStatus is LoadStatus.FailedToLoad){
                    // show error message
                    val errorMessage = loadStatus.error.message.toString()
                    Snackbar.make(activityDownloadBinding.layout, errorMessage,Snackbar.LENGTH_SHORT).show()
                    Log.e(this@DownloadActivity.packageName, errorMessage)
                }
            }
        }
    }
}
