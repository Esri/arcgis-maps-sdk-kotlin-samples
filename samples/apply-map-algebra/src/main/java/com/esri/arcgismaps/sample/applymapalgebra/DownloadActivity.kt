package com.esri.arcgismaps.sample.applymapalgebra

import android.content.Intent
import android.os.Bundle
import com.esri.arcgismaps.sample.sampleslib.DownloaderActivity

class DownloadActivity : DownloaderActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        downloadAndStartSample(
            Intent(this, MainActivity::class.java),
            getString(R.string.apply_map_algebra_app_name),
            listOf(
                "https://www.arcgis.com/home/item.html?id=aa97788593e34a32bcaae33947fdc271"
            )
        )
    }
}
