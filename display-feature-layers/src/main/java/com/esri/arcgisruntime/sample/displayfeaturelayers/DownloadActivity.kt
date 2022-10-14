package com.esri.arcgisruntime.sample.displayfeaturelayers

import android.content.Intent
import android.os.Bundle
import com.esri.arcgisruntime.sample.sampleslib.DownloaderActivity

class DownloadActivity : DownloaderActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        doDownloadThenStartSample(
            Intent(this, MainActivity::class.java),
            // get the file path of the (.mmpk) file
            getExternalFilesDir(null)?.path + getString(R.string.geodatabase_la_trails),
            // ArcGIS Portal item containing the .mmpk mobile map package
            "https://www.arcgis.com/home/item.html?id=2b0f9e17105847809dfeb04e3cad69e0"
        )
    }
}
