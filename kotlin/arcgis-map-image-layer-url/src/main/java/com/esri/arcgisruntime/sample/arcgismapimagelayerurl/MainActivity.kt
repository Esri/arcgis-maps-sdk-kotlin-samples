package com.esri.arcgisruntime.sample.arcgismapimagelayerurl

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import arcgisruntime.ApiKey
import arcgisruntime.ArcGISRuntimeEnvironment
import arcgisruntime.mapping.ArcGISMap
import arcgisruntime.mapping.layers.ArcGISMapImageLayer
import com.esri.arcgisruntime.sample.arcgismapimagelayerurl.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var activityMainBinding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISRuntimeEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)

        activityMainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        lifecycle.addObserver(activityMainBinding.mapView)


        // create a MapImageLayer with dynamically generated map images
        val mapImageLayer = ArcGISMapImageLayer("https://sampleserver5.arcgisonline.com/arcgis/rest/services/Elevation/WorldElevations/MapServer")
        // create an empty map instance
        val map = ArcGISMap()
        // add map image layer as operational layer
        map.operationalLayers.add(mapImageLayer)
        // set the map to be displayed in this view
        activityMainBinding.mapView.map = map
    }
}
