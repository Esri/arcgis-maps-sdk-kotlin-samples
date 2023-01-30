package com.esri.arcgismaps.sample.findrouteintransportnetwork

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
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
                //A zip file containing an offline routing network and .tpkx basemap
                "https://arcgisruntime.maps.arcgis.com/home/item.html?id=df193653ed39449195af0c9725701dca"
            )

        )
    }
}
