package com.esri.arcgismaps.sample.generategeodatabasereplicafromfeatureservice

import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import com.esri.arcgismaps.sample.sampleslib.DownloaderActivity

class DownloadActivity : DownloaderActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        downloadAndStartSample(Intent(this, MainActivity::class.java),
            getString(R.string.app_name),
            listOf("https://arcgisruntime.maps.arcgis.com/home/item.html?id=e4a398afe9a945f3b0f4dca1e4faccb5"))
    }
}