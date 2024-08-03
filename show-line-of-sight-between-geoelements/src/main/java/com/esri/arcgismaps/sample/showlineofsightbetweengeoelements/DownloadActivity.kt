package com.esri.arcgismaps.sample.showlineofsightbetweengeoelements

import android.content.Intent
import android.os.Bundle
import com.esri.arcgismaps.sample.sampleslib.DownloaderActivity

class DownloadActivity : DownloaderActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        downloadAndStartSample(
            Intent(this, MainActivity::class.java),
            // get the app name of the sample
            getString(R.string.app_name),
            listOf(
                "https://www.arcgis.com/home/item.html?id=3af5cfec0fd24dac8d88aea679027cb9"
            )

        )
    }
}
