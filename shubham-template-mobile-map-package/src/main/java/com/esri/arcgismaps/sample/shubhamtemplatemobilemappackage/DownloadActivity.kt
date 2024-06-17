package com.esri.arcgismaps.sample.shubhamtemplatemobilemappackage

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
                // ArcGIS Portal item containing the .mspk mobile scene package
                "https://www.arcgis.com/home/item.html?id=e1f3a7254cb845b09450f54937c16061"
            )
        )
    }
}
