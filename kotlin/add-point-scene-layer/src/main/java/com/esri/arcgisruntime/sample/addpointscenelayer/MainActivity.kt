package com.esri.arcgisruntime.sample.addpointscenelayer

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import arcgisruntime.ApiKey
import arcgisruntime.ArcGISRuntimeEnvironment
import arcgisruntime.mapping.ArcGISScene
import arcgisruntime.mapping.ArcGISTiledElevationSource
import arcgisruntime.mapping.BasemapStyle
import arcgisruntime.mapping.Surface
import arcgisruntime.mapping.layers.ArcGISSceneLayer
import com.esri.arcgisruntime.sample.addpointscenelayer.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var activityMainBinding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISRuntimeEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)

        activityMainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        lifecycle.addObserver(activityMainBinding.sceneView)

        // create a scene with a basemap and add it to the sceneView
        val scene = ArcGISScene(BasemapStyle.ArcGISImagery)

        // set the base surface with world elevation
        val surface = Surface()
        surface.elevationSources.add(ArcGISTiledElevationSource(getString(R.string.elevation_image_service)))
        scene.baseSurface = surface

        // add a point scene layer with points at world airport location
        val pointSceneLayer = ArcGISSceneLayer(getString(R.string.world_airports_scene_layer))
        scene.operationalLayers.add(pointSceneLayer)

        // add the scene to the sceneView
        activityMainBinding.sceneView.scene = scene
    }
}
