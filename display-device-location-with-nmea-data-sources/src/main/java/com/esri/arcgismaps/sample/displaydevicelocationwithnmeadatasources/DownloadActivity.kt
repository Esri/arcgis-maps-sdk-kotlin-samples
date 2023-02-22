package com.esri.arcgismaps.sample.displaydevicelocationwithnmeadatasources

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
                // ArcGIS Portal item containing the Redlands.nmea
                // which features a vehicle driving around southern Redlands, CA.
                "https://www.arcgis.com/home/item.html?id=d5bad9f4fee9483791e405880fb466da"
            )

        )
    }
}
