package com.esri.arcgismaps.sample.addcustomdynamicentitydatasource

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
            // ArcGIS Portal item containing the json file of observations of marine vessels in
            // the Pacific North West
            listOf(
                "https://www.arcgis.com/home/item.html?id=a8a942c228af4fac96baa78ad60f511f"
            )
        )
    }
}
