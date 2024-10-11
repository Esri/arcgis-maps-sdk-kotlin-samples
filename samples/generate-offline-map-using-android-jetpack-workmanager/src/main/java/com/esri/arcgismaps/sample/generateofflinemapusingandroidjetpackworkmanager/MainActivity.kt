/*
 * Copyright 2023 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.esri.arcgismaps.sample.generateofflinemapusingandroidjetpackworkmanager

import android.Manifest.permission.POST_NOTIFICATIONS
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.asFlow
import androidx.lifecycle.lifecycleScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.Color
import com.arcgismaps.geometry.Envelope
import com.arcgismaps.geometry.Geometry
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.MobileMapPackage
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.ScreenCoordinate
import com.arcgismaps.portal.Portal
import com.arcgismaps.tasks.offlinemaptask.GenerateOfflineMapJob
import com.arcgismaps.tasks.offlinemaptask.GenerateOfflineMapParameters
import com.arcgismaps.tasks.offlinemaptask.OfflineMapTask
import com.esri.arcgismaps.sample.generateofflinemapusingandroidjetpackworkmanager.databinding.GenerateOfflineMapUsingAndroidJetpackWorkmanagerActivityMainBinding
import com.esri.arcgismaps.sample.generateofflinemapusingandroidjetpackworkmanager.databinding.OfflineJobProgressDialogLayoutBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.io.File
import kotlin.random.Random

// data parameter keys for the WorkManager
// key for the NotificationId parameter
const val notificationIdParameter = "NotificationId"

// key for the json job file path
const val jobParameter = "JsonJobPath"

class MainActivity : AppCompatActivity() {

    // set up data binding for the activity
    private val activityMainBinding: GenerateOfflineMapUsingAndroidJetpackWorkmanagerActivityMainBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.generate_offline_map_using_android_jetpack_workmanager_activity_main)
    }

    private val mapView by lazy {
        activityMainBinding.mapView
    }

    private val takeMapOfflineButton by lazy {
        activityMainBinding.takeMapOfflineButton
    }

    private val resetMapButton by lazy {
        activityMainBinding.resetButton
    }

    // instance of the WorkManager
    private val workManager by lazy {
        WorkManager.getInstance(this)
    }

    // file path to store the offline map package
    private val offlineMapPath by lazy {
        getExternalFilesDir(null)?.path + getString(R.string.offlineMapFile)
    }

    // shows the offline map job loading progress
    private val progressLayout by lazy {
        OfflineJobProgressDialogLayoutBinding.inflate(layoutInflater)
    }

    // alert dialog view for the progress layout
    private val progressDialog by lazy {
        createProgressDialog().create()
    }

    // used to uniquely identify the work request so that only one worker is active at a time
    // also allows us to query and observe work progress
    private val uniqueWorkName = "ArcgisMaps.Sample.OfflineMapJob.Worker"

    // create a graphic overlay
    private val graphicsOverlay = GraphicsOverlay()

    // represents bounds of the downloadable area of the map
    private val downloadArea = Graphic(
        symbol = SimpleLineSymbol(SimpleLineSymbolStyle.Solid, Color.red, 2F)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // request notifications permission
        requestNotificationPermission()

        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.ACCESS_TOKEN)
        lifecycle.addObserver(mapView)

        // set up the portal item to take offline
        setUpMapView()

        // clear the preview map and display the Portal Item
        resetMapButton.setOnClickListener {
            // enable offline button
            takeMapOfflineButton.isEnabled = true
            resetMapButton.isEnabled = false
            // clear graphic overlays
            graphicsOverlay.graphics.clear()
            mapView.graphicsOverlays.clear()

            // set up the portal item to take offline
            setUpMapView()
        }
    }

    /**
     * Sets up a portal item and displays map area to take offline
     */
    private fun setUpMapView() {
        // create a portal item with the itemId of the web map
        val portal = Portal(getString(R.string.portal_url))
        val portalItem = PortalItem(portal, getString(R.string.item_id))

        // add the graphic to the graphics overlay when it is created
        graphicsOverlay.graphics.add(downloadArea)
        // create and add a map with with portal item
        val map = ArcGISMap(portalItem)
        // apply mapview assignments
        mapView.apply {
            this.map = map
            graphicsOverlays.add(graphicsOverlay)
        }

        lifecycleScope.launch {
            map.load().onFailure {
                // show an error and return if the map load failed
                showMessage("Error loading map: ${it.message}")
                return@launch
            }

            // enable the take map offline button only after the map is loaded
            takeMapOfflineButton.isEnabled = true

            // get the Control Valve layer from the map's operational layers
            val operationalLayer =
                map.operationalLayers.firstOrNull { layer ->
                    layer.name == "Control Valve"
                } ?: return@launch showMessage("Error finding Control Valve layer")

            // limit the map scale to the layer's scale
            map.maxScale = operationalLayer.maxScale ?: 0.0
            map.minScale = operationalLayer.minScale ?: 0.0

            mapView.viewpointChanged.collect {
                // upper left corner of the area to take offline
                val minScreenPoint = ScreenCoordinate(200.0, 200.0)
                // lower right corner of the downloaded area
                val maxScreenPoint = ScreenCoordinate(
                    mapView.width - 200.0,
                    mapView.height - 200.0
                )
                // convert screen points to map points
                val minPoint = mapView.screenToLocation(minScreenPoint) ?: return@collect
                val maxPoint = mapView.screenToLocation(maxScreenPoint) ?: return@collect
                // use the points to define and set an envelope for the downloadArea graphic
                val envelope = Envelope(minPoint, maxPoint)
                downloadArea.geometry = envelope
            }
        }

        // set onclick listener for the takeMapOfflineButton
        takeMapOfflineButton.setOnClickListener {
            // if the downloadArea's geometry is not null
            downloadArea.geometry?.let { geometry ->
                // create an OfflineMapJob
                val offlineMapJob = createOfflineMapJob(map, geometry)
                // start the OfflineMapJob
                startOfflineMapJob(offlineMapJob)
                // show the progress dialog
                progressDialog.show()
                // disable the button
                takeMapOfflineButton.isEnabled = false
            }
        }

        // start observing the worker's progress and status
        observeWorkStatus()
    }

    /**
     * Creates and returns a new GenerateOfflineMapJob for the [map] and its [areaOfInterest]
     */
    private fun createOfflineMapJob(
        map: ArcGISMap,
        areaOfInterest: Geometry
    ): GenerateOfflineMapJob {
        // check and delete if the offline map package file already exists
        File(offlineMapPath).deleteRecursively()
        // specify the min scale and max scale as parameters
        val maxScale = map.maxScale ?: 0.0
        var minScale = map.minScale ?: 0.0
        // minScale must always be larger than maxScale
        if (minScale <= maxScale) {
            minScale = maxScale + 1
        }
        // set the offline map parameters
        val generateOfflineMapParameters = GenerateOfflineMapParameters(
            areaOfInterest,
            minScale,
            maxScale
        ).apply {
            // set job to cancel on any errors
            continueOnErrors = false
        }
        // create an offline map task with the map
        val offlineMapTask = OfflineMapTask(map)
        // create an offline map job with the download directory path and parameters and
        // return the job
        return offlineMapTask.createGenerateOfflineMapJob(
            generateOfflineMapParameters,
            offlineMapPath
        )
    }

    /**
     * Starts the [offlineMapJob] using OfflineJobWorker with WorkManager. The [offlineMapJob] is
     * serialized into a json file and the uri is passed to the OfflineJobWorker, since WorkManager
     * enforces a MAX_DATA_BYTES for the WorkRequest's data
     */
    private fun startOfflineMapJob(offlineMapJob: GenerateOfflineMapJob) {
        // create a temporary file path to save the offlineMapJob json file
        val offlineJobJsonPath = getExternalFilesDir(null)?.path +
                getString(R.string.offlineJobJsonFile)

        // create the json file
        val offlineJobJsonFile = File(offlineJobJsonPath)
        // serialize the offlineMapJob into the file
        offlineJobJsonFile.writeText(offlineMapJob.toJson())

        // create a non-zero notification id for the OfflineJobWorker
        // this id will be used to post or update any progress/status notifications
        val notificationId = Random.Default.nextInt(1, 100)

        // create a one-time work request with an instance of OfflineJobWorker
        val workRequest = OneTimeWorkRequestBuilder<OfflineJobWorker>()
            // run it as an expedited work
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            // add the input data
            .setInputData(
                // add the notificationId and the json file path as a key/value pair
                workDataOf(
                    notificationIdParameter to notificationId,
                    jobParameter to offlineJobJsonFile.absolutePath
                )
            ).build()

        // enqueue the work request to run as a unique work with the uniqueWorkName, so that
        // only one instance of OfflineJobWorker is running at any time
        // if any new work request with the uniqueWorkName is enqueued, it replaces any existing
        // ones that are active
        workManager.enqueueUniqueWork(uniqueWorkName, ExistingWorkPolicy.REPLACE, workRequest)
    }

    /**
     * Starts observing any running or completed OfflineJobWorker work requests by capturing the
     * LiveData as a flow. The flow starts receiving updates when the activity is in started
     * or resumed state. This allows the application to capture immediate progress when
     * in foreground and latest progress when the app resumes or restarts.
     */
    private fun observeWorkStatus() {
        // get the livedata observer of the unique work as a flow
        val liveDataFlow = workManager.getWorkInfosForUniqueWorkLiveData(uniqueWorkName).asFlow()

        lifecycleScope.launch {
            // collect the live data flow to get the latest work info list
            liveDataFlow.collect { workInfoList ->
                if (workInfoList.isNotEmpty()) {
                    // fetch the first work info as we only ever run one work request at any time
                    val workInfo = workInfoList[0]
                    // check the current state of the work request
                    when (workInfo.state) {
                        // if work completed successfully
                        WorkInfo.State.SUCCEEDED -> {
                            // load and display the offline map
                            displayOfflineMap()
                            // dismiss the progress dialog
                            if (progressDialog.isShowing) {
                                progressDialog.dismiss()
                            }
                        }
                        // if the work failed or was cancelled
                        WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -> {
                            // show an error message based on if it was cancelled or failed
                            if (workInfo.state == WorkInfo.State.FAILED) {
                                showMessage("Error generating offline map")
                            } else {
                                showMessage("Cancelled offline map generation")
                            }
                            // dismiss the progress dialog
                            if (progressDialog.isShowing) {
                                progressDialog.dismiss()
                            }
                            // enable the takeMapOfflineButton
                            takeMapOfflineButton.isEnabled = true
                            // this removes the completed WorkInfo from the WorkManager's database
                            // otherwise, the observer will emit the WorkInfo on every launch
                            // until WorkManager auto-prunes
                            workManager.pruneWork()
                        }
                        // if the work is currently in progress
                        WorkInfo.State.RUNNING -> {
                            // get the current progress value
                            val value = workInfo.progress.getInt("Progress", 0)
                            // update the progress bar and progress text
                            progressLayout.progressBar.progress = value
                            progressLayout.progressTextView.text = "$value%"
                            // shows the progress dialog if the app is relaunched and the
                            // dialog is not visible
                            if (!progressDialog.isShowing) {
                                progressDialog.show()
                            }
                        }
                        else -> { /* don't have to handle other states */
                        }
                    }
                }
            }
        }
    }

    /**
     * Loads the offline map package into the mapView
     */
    private fun displayOfflineMap() {
        lifecycleScope.launch {
            // check if the offline map package file exists
            if (File(offlineMapPath).exists()) {
                // load it as a MobileMapPackage
                val mapPackage = MobileMapPackage(offlineMapPath)
                mapPackage.load().onFailure {
                    // if the load fails, show an error and return
                    showMessage("Error loading map package: ${it.message}")
                    return@launch
                }
                // add the map from the mobile map package to the MapView
                mapView.map = mapPackage.maps.first()
                // clear all the drawn graphics
                graphicsOverlay.graphics.clear()
                // disable the button to take the map offline once the offline map is showing
                takeMapOfflineButton.isEnabled = false
                resetMapButton.isEnabled = true
                // this removes the completed WorkInfo from the WorkManager's database
                // otherwise, the observer will emit the WorkInfo on every launch
                // until WorkManager auto-prunes
                workManager.pruneWork()
                // display the offline map loaded message
                showMessage("Loaded offline map. Map saved at: $offlineMapPath")
            } else {
                showMessage("Offline map does not exists at path: $offlineMapPath")
            }
        }
    }

    /**
     * Creates a progress dialog to show the OfflineMapJob worker progress. It cancels all the
     * running workers when the dialog is cancelled
     */
    private fun createProgressDialog(): MaterialAlertDialogBuilder {
        // build and return a new alert dialog
        return MaterialAlertDialogBuilder(this).apply {
            // set it title
            setTitle(getString(R.string.dialog_title))
            // allow it to be cancellable
            setCancelable(false)
            // set negative button configuration
            setNegativeButton("Cancel") { _, _ ->
                // cancel all the running work
                workManager.cancelAllWork()
            }
            // removes parent of the progressDialog layout, if previously assigned
            progressLayout.root.parent?.let { parent ->
                (parent as ViewGroup).removeAllViews()
            }
            // set the progressDialog Layout to this alert dialog
            setView(progressLayout.root)
        }
    }

    /**
     * Request Post Notifications permission for API level 33+
     * https://developer.android.com/develop/ui/views/notifications/notification-permission
     */
    private fun requestNotificationPermission() {
        // request notification permission only for android versions >= 33
        if (Build.VERSION.SDK_INT >= 33) {
            // check if push notifications permission is granted
            val permissionCheckPostNotifications =
                ContextCompat.checkSelfPermission(this@MainActivity, POST_NOTIFICATIONS) ==
                        PackageManager.PERMISSION_GRANTED

            // if permission is not already granted, request permission from the user
            if (!permissionCheckPostNotifications) {
                ActivityCompat.requestPermissions(
                    this@MainActivity,
                    arrayOf(POST_NOTIFICATIONS),
                    2
                )
            }
        }
    }

    /**
     * Handle the permissions request response.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_DENIED) {
            Snackbar.make(
                mapView,
                "Notification permissions required to show progress!",
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // dismiss the dialog when the activity is destroyed
        progressDialog.dismiss()
    }

    private fun showMessage(message: String) {
        Log.e(localClassName, message)
        Snackbar.make(mapView, message, Snackbar.LENGTH_SHORT).show()
    }
}
