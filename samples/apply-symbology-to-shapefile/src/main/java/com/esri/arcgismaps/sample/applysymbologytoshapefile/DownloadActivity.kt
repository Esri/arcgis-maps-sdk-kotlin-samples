package com.esri.arcgismaps.sample.applysymbologytoshapefile

import android.content.Intent
import android.os.Bundle
import com.esri.arcgismaps.sample.sampleslib.DownloaderActivity

class DownloadActivity : DownloaderActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        downloadAndStartSample(
            Intent(this, MainActivity::class.java),
            getString(R.string.apply_symbology_to_shapefile_app_name),
            listOf(
                "https://www.arcgis.com/home/item.html?id=d98b3e5293834c5f852f13c569930caa"
            )
        )
    }
}