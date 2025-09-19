package com.esri.arcgismaps.sample.applydictionaryrenderertographicsoverlay

import android.content.Intent
import android.os.Bundle
import com.esri.arcgismaps.sample.sampleslib.DownloaderActivity

class DownloadActivity : DownloaderActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        downloadAndStartSample(
            Intent(this, MainActivity::class.java),
            getString(R.string.apply_dictionary_renderer_to_graphics_overlay_app_name),
            listOf(
                "https://www.arcgis.com/home/item.html?id=8776cfc26eed4485a03de6316826384c"
            )
        )
    }
}
