package com.esri.arcgismaps.kotlin.sampleviewer

import android.app.Application
import com.esri.arcgismaps.kotlin.sampleviewer.model.DefaultSampleInfoRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SampleViewerApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        CoroutineScope(Dispatchers.IO).launch {
            // Load the repository once at app launch
            DefaultSampleInfoRepository.load(applicationContext)
        }
    }
}
