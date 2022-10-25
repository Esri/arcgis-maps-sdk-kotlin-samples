package com.esri.arcgisruntime.sample.displayfeaturelayers

import android.content.Intent
import android.os.Bundle
import com.esri.arcgisruntime.sample.sampleslib.DownloaderActivity

class DownloadActivity : DownloaderActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        downloadAndStartSample(
            Intent(this, MainActivity::class.java),
            // get the download path of the sample
            getExternalFilesDir(null)?.path.toString(),
            listOf<String>(
                // ArcGIS Portal item containing the .mmpk mobile map package
                "https://www.arcgis.com/home/item.html?id=2b0f9e17105847809dfeb04e3cad69e0"
            )

        )
    }
}
