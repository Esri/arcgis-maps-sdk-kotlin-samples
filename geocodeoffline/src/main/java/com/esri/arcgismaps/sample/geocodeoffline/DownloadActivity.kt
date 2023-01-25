package com.esri.arcgismaps.sample.geocodeoffline

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
                // A .tpkx Tile Package file covering the San Diego, CA, USA area
                "https://www.arcgis.com/home/item.html?id=22c3083d4fa74e3e9b25adfc9f8c0496",
                // San Diego Locator Offline Dataset
                "https://www.arcgis.com/home/item.html?id=3424d442ebe54f3cbf34462382d3aebe"
            )
        )
    }
}
