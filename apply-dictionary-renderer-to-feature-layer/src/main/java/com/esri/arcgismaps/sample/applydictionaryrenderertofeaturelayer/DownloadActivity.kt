package com.esri.arcgismaps.sample.applydictionaryrenderertofeaturelayer

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
                // A stylx file that incorporates the MIL-STD-2525D symbol dictionary
//                "https://www.arcgis.com/home/item.html?id=e34835bf5ec5430da7cf16bb8c0b075c",
                "https://arcgisruntime.maps.arcgis.com/home/item.html?id=4581a9a92c214240a79e15482a2e8349",
                // A mobile geodatabase created from the ArcGIS for Defense Military Overlay template
                "https://www.arcgis.com/home/item.html?id=e0d41b4b409a49a5a7ba11939d8535dc"
            )
        )
    }
}