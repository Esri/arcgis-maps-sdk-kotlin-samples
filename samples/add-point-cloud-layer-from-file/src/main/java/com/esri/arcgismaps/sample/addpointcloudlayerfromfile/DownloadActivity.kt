package com.esri.arcgismaps.sample.addpointcloudlayerfromfile

import android.content.Intent
import android.os.Bundle
import com.esri.arcgismaps.sample.sampleslib.DownloaderActivity

class DownloadActivity : DownloaderActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        downloadAndStartSample(
            Intent(this, MainActivity::class.java),
            getString(R.string.add_point_cloud_layer_from_file_app_name),
            listOf(
                "https://arcgisruntime.maps.arcgis.com/home/item.html?id=34da965ca51d4c68aa9b3a38edb29e00"
            )
        )
    }
}
