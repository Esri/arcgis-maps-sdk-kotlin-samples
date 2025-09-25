package com.esri.arcgismaps.sample.stylefeatureswithcustomdictionary

import android.content.Intent
import android.os.Bundle
import com.esri.arcgismaps.sample.sampleslib.DownloaderActivity

class DownloadActivity : DownloaderActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        downloadAndStartSample(
            Intent(this, MainActivity::class.java),
            getString(R.string.style_features_with_custom_dictionary_app_name),
            listOf(
                "https://www.arcgis.com/home/item.html?id=751138a2e0844e06853522d54103222a",
            )
        )
    }
}
