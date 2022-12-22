package com.esri.arcgismaps.sample.addfeatureswithcontingentvalues

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
                // Geodatabase containing bird nests defined with contingent values
                "https://www.arcgis.com/home/item.html?id=e12b54ea799f4606a2712157cf9f6e41",
                // Vector tile package of the Fillmore area
                "https://www.arcgis.com/home/item.html?id=b5106355f1634b8996e634c04b6a930a"
            )
        )
    }
}
