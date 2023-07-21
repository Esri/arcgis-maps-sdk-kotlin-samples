package com.esri.arcgismaps.sample.displayscenefrommobilescenepackage

import android.content.Intent
import android.os.Bundle
import com.esri.arcgismaps.sample.sampleslib.DownloaderActivity

class DownloadActivity: DownloaderActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        downloadAndStartSample(
            Intent(this, MainActivity::class.java),
            // get the app name of the sample
            getString(R.string.app_name),
            listOf(
                // ArcGIS Portal item containing the .mspk mobile scene package
                "https://www.arcgis.com/home/item.html?id=7dd2f97bb007466ea939160d0de96a9d"
            )
        )
    }
}
