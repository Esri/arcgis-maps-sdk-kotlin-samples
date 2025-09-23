package com.esri.arcgismaps.sample.applycolormaprenderertoraster

import android.content.Intent
import android.os.Bundle
import com.esri.arcgismaps.sample.sampleslib.DownloaderActivity

class DownloadActivity : DownloaderActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        downloadAndStartSample(
            Intent(this, MainActivity::class.java),
            getString(R.string.apply_colormap_renderer_to_raster_app_name),
            listOf(
                "https://www.arcgis.com/home/item.html?id=cc68728b5904403ba637e1f1cd2995ae"
            )
        )
    }
}
