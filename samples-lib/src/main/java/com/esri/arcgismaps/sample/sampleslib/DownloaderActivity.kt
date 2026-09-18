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

package com.esri.arcgismaps.sample.sampleslib

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.esri.arcgismaps.sample.sampleslib.components.DownloaderScreen
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

open class DownloaderActivity : ComponentActivity() {

    private val downloadViewModel: DownloadViewModel by viewModels()
    private var mainActivity: Intent? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sampleName = requireNotNull(
            intent.getStringExtra(EXTRA_SAMPLE_NAME)
        )

        val provisionUrls = requireNotNull(
            intent.getStringArrayListExtra(EXTRA_PROVISION_URLS)
        )

        val mainActivityClassName = requireNotNull(
            intent.getStringExtra(EXTRA_MAIN_ACTIVITY)
        )

        val targetActivity = Class
            .forName(mainActivityClassName)
            .asSubclass(Activity::class.java)

        mainActivity = Intent(this, targetActivity)

        downloadViewModel.configure(
            sampleName = sampleName,
            provisionUrls = provisionUrls
        )
        setContent {
            MaterialTheme {
                val uiState by downloadViewModel.uiState.collectAsState()

                LaunchedEffect(Unit) {
                    downloadViewModel.launchSample.collect {
                        mainActivity?.let { intent ->
                            startActivity(Intent(intent))
                            finish()
                        }
                    }
                }
                DownloaderScreen(
                    uiState = uiState,
                    onDownload = downloadViewModel::startDownload,
                    onCancel = {
                        lifecycleScope.launch {
                            downloadViewModel.cancelDownload()
                            finish()
                        }
                    },
                    onExit = ::finish
                )
            }
        }
    }

    companion object {
        private const val EXTRA_SAMPLE_ID = "extra_sample_id"
        private const val EXTRA_SAMPLE_NAME = "extra_sample_name"
        private const val EXTRA_PROVISION_URLS = "extra_provision_urls"
        private const val EXTRA_MAIN_ACTIVITY = "extra_main_activity"
        fun createIntent(
            context: Context,
            sampleId: String,
            sampleName: String,
            provisionUrls: List<String>,
            mainActivityClassName: String
        ): Intent {
            return Intent(context, DownloaderActivity::class.java).apply {
                putExtra(EXTRA_SAMPLE_ID, sampleId)
                putExtra(EXTRA_SAMPLE_NAME, sampleName)
                putStringArrayListExtra(
                    EXTRA_PROVISION_URLS,
                    ArrayList(provisionUrls)
                )
                putExtra(EXTRA_MAIN_ACTIVITY, mainActivityClassName)
            }
        }
    }
}

