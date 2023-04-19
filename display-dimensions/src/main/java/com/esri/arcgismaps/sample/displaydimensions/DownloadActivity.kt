package com.esri.arcgismaps.sample.displaydimensions

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
                // ArcGIS Portal item containing the mmpk file which is a  section of the
                // high-voltage electricity transmission network around Edinburgh, Scotland.
                "https://www.arcgis.com/home/item.html?id=f5ff6f5556a945bca87ca513b8729a1e"
            )
        )
    }
}
