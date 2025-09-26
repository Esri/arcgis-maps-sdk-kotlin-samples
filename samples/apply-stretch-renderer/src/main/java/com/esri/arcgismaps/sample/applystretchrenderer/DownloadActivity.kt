package com.esri.arcgismaps.sample.applystretchrenderer

import android.content.Intent
import android.os.Bundle
import com.esri.arcgismaps.sample.sampleslib.DownloaderActivity

class DownloadActivity : DownloaderActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        downloadAndStartSample(
            Intent(this, MainActivity::class.java),
            getString(R.string.apply_stretch_renderer_app_name),
            listOf(
                "https://www.arcgis.com/home/item.html?id=7c4c679ab06a4df19dc497f577f111bd"
            )
        )
    }
}
