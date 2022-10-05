package com.esri.arcgisruntime.sample.displaymapfrommobilemappackage

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.esri.arcgisruntime.sample.sampleslib.SampleActivity
import kotlinx.coroutines.launch

class DownloadActivity : SampleActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_download)

        // get the file path of the (.mmpk) file
        val filePath = getExternalFilesDir(null)?.path + getString(R.string.yellowstone_mmpk)

        // start the download manager to automatically add the .mmpk file to the app
        // alternatively, you can use ADB/Device File Explorer
        lifecycleScope.launch {
            sampleDownloadManager(MainActivity().provisionURL, filePath).collect {
                // download complete, resuming sample
                startActivity(Intent(this@DownloadActivity, MainActivity::class.java))
                finish()
            }
        }
    }
}
