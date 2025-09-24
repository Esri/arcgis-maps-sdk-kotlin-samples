package com.esri.arcgismaps.sample.stylepointwithdistancecompositescenesymbol

import android.content.Intent
import android.os.Bundle
import com.esri.arcgismaps.sample.sampleslib.DownloaderActivity

class DownloadActivity : DownloaderActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        downloadAndStartSample(
            Intent(this, MainActivity::class.java),
            getString(R.string.style_point_with_distance_composite_scene_symbol_app_name),
            listOf(
                "https://www.arcgis.com/home/item.html?id=681d6f7694644709a7c830ec57a2d72b"
            )
        )
    }
}
