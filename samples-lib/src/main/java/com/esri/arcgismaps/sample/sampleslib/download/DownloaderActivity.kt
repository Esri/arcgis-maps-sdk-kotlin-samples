package com.esri.arcgismaps.sample.sampleslib.download

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

        val provisionIDs = requireNotNull(
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
            provisionUrls = provisionIDs
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
        private const val EXTRA_SAMPLE_NAME = "extra_sample_name"
        private const val EXTRA_PROVISION_URLS = "extra_provision_urls"
        private const val EXTRA_MAIN_ACTIVITY = "extra_main_activity"
        fun createIntent(
            context: Context,
            sampleName: String,
            provisionUrls: List<String>,
            mainActivityClassName: String
        ): Intent {
            return Intent(context, DownloaderActivity::class.java).apply {
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