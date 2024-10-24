package com.esri.arcgismaps.kotlin.sampleviewer

import android.app.Application
import com.esri.arcgismaps.kotlin.sampleviewer.model.DefaultSampleInfoRepository
import kotlinx.coroutines.runBlocking

class SampleViewerApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        runBlocking {
            // Load the repository once at app launch
            DefaultSampleInfoRepository.load(applicationContext)
        }
    }
}