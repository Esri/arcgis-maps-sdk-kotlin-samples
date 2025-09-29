package com.esri.arcgismaps.sample.showmobilemappackageexpirationdate

import android.content.Intent
import android.os.Bundle
import com.esri.arcgismaps.sample.sampleslib.DownloaderActivity

class DownloadActivity : DownloaderActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        downloadAndStartSample(
            Intent(this, MainActivity::class.java),
            getString(R.string.show_mobile_map_package_expiration_date_app_name),
            listOf(
                "https://www.arcgis.com/home/item.html?id=174150279af74a2ba6f8b87a567f480b"
            )
        )
    }
}