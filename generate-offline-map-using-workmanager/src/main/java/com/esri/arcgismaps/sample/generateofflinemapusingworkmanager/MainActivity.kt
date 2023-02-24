/* Copyright 2022 Esri
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

package com.esri.arcgismaps.sample.generateofflinemapusingworkmanager

import android.Manifest.permission.POST_NOTIFICATIONS
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.asFlow
import androidx.work.*
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.Color
import com.arcgismaps.geometry.Envelope
import com.arcgismaps.geometry.Geometry
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.MobileMapPackage
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.ScreenCoordinate
import com.arcgismaps.portal.Portal
import com.arcgismaps.portal.PortalItem
import com.arcgismaps.tasks.offlinemaptask.GenerateOfflineMapJob
import com.arcgismaps.tasks.offlinemaptask.GenerateOfflineMapParameters
import com.arcgismaps.tasks.offlinemaptask.OfflineMapTask
import com.esri.arcgismaps.sample.generateofflinemapusingworkmanager.databinding.ActivityMainBinding
import com.esri.arcgismaps.sample.generateofflinemapusingworkmanager.databinding.GenerateOfflineMapDialogLayoutBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.io.File
import kotlin.random.Random

// Data parameter keys for the WorkManager
// key for the NotificationId parameter
const val notificationIdParameter = "NotificationId"

// key for the json job file path
const val jobParameter = "JsonJobPath"

class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.java.simpleName

    // set up data binding for the activity
    private val activityMainBinding: ActivityMainBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.activity_main)
    }

    private val mapView by lazy {
        activityMainBinding.mapView
    }

    private val takeMapOfflineButton by lazy {
        activityMainBinding.takeMapOfflineButton
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
        GenerateOfflineMapDialogLayoutBinding.inflate(layoutInflater)
    }

    // dialog view for the progress bar
    private val progressDialog by lazy {
        createProgressDialog()
    }

    // used to uniquely identify the work request so that only one worker is active at a time
    // also allows us to query and observe work progress
    private val uniqueWorkName = "offlineJobWorker"

    // creates a graphic overlay
    private val graphicsOverlay = GraphicsOverlay()

    // graphic representing bounds of the downloadable area of the map
    private val downloadArea = Graphic()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // request notifications permission
        requestNotificationPermission()

        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)
        lifecycle.addObserver(mapView)

        // create a portal item with the itemId of the web map
        val portal = Portal(getString(R.string.portal_url))
        val portalItem = PortalItem(portal, getString(R.string.item_id))

        // create a symbol to show a box around the extent we want to download
        downloadArea.symbol = SimpleLineSymbol(SimpleLineSymbolStyle.Solid, Color.red, 2F)
        // add the graphic to the graphics overlay when it is created
        graphicsOverlay.graphics.add(downloadArea)
        // create and add a map with a navigation night basemap style
        val map = ArcGISMap(portalItem)
        // apply mapview assignments
        mapView.apply {
            this.map = map
            graphicsOverlays.add(graphicsOverlay)
        }

        lifecycleScope.launch {
            map.load().onFailure {
                showMessage("Error loading map: ${it.message}")
                return@launch
            }

            // enable the take map offline button only after the map is loaded
            takeMapOfflineButton.isEnabled = true

            // limit the map scale to the largest layer scale
            map.maxScale = map.operationalLayers[6].maxScale ?: 0.0
            map.minScale = map.operationalLayers[6].minScale ?: 0.0

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
                // use the points to define and return an envelope
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
        // specify the min scale, and max scale as parameters
        val maxScale = map.maxScale
        // minScale must always be larger than maxScale
        val minScale = if (map.minScale <= maxScale) {
            maxScale + 1
        } else {
            map.minScale
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
        // serialize the offlineMapJob json into the file
        offlineJobJsonFile.writeText(offlineMapJob.toJson())

        // create a random non-zero notification id for the OfflineJobWorker
        // this id will be used to post or update any progress/status notifications
        val notificationId = Random.Default.nextInt(1, 100)

        // create the OfflineJobWorker work request as a one time work request
        val workRequest = OneTimeWorkRequestBuilder<OfflineJobWorker>()
            // run it as an expedited work
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            // add the input data
            .setInputData(
                // add the notificationId and the job's json file path as a key/value pair
                workDataOf(
                    notificationIdParameter to notificationId,
                    jobParameter to offlineJobJsonFile.absolutePath
                )
            )
            .build()

        // enqueue the work request to run as a unique work with the uniqueWorkName, so that
        // only one instance of OfflineJobWorker is running at any time
        // if any new uniqueWorkName work request is enqueued, it replaces any existing ones
        // that are running
        workManager.enqueueUniqueWork(uniqueWorkName, ExistingWorkPolicy.REPLACE, workRequest)
    }

    /**
     * Starts observing any running or completed OfflineJobWorker work requests by capturing the
     * LiveData as a flow. The flow starts receiving updates when the activity is in started
     * or resumed state. This allows the application to capture current immediate progress when
     * in foreground and latest progress when the app resumes or restarts.
     */
    private fun observeWorkStatus() {
        // get the livedata observer of the unique work as a flow
        val liveDataFlow = workManager.getWorkInfosForUniqueWorkLiveData(uniqueWorkName).asFlow()

        lifecycleScope.launch {
            // collect the live data flow to get the latest work info list
            liveDataFlow.collect { workInfoList ->
                if (workInfoList.size > 0) {
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
                        // if the work failed
                        WorkInfo.State.FAILED -> {
                            // show an error message
                            showMessage("Error generating offline map")
                            // dismiss the progress dialog
                            if (progressDialog.isShowing) {
                                progressDialog.dismiss()
                            }
                            // enable the takeMapOfflineButton
                            takeMapOfflineButton.isEnabled = true
                            // this removes the completed WorkInfo from the WorkManager's database
                            // Otherwise, the observer will emit the WorkInfo on every launch
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
                            // shows the progress dialog if the app is relaunched and the progress
                            // dialog is not visible
                            if (!progressDialog.isShowing) {
                                progressDialog.show()
                            }
                        }
                        else -> { /* other states are not relevant */
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
                // hide the takeMapOfflineButton
                takeMapOfflineButton.visibility = View.GONE
                // this removes the completed WorkInfo from the WorkManager's database
                // Otherwise, the observer will emit the WorkInfo on every launch
                // until WorkManager auto-prunes
                workManager.pruneWork()
                // display the offline map loaded message
                showMessage("Loaded offline map. Map saved at: $offlineMapPath")
            }
        }
    }

    /**
     * Creates a progress dialog to show the OfflineMapJob progress. It cancels all the
     * running work on when the dialog is cancelled
     */
    private fun createProgressDialog(): AlertDialog {
        // build and return a new alert dialog
        return AlertDialog.Builder(this).apply {
            // setting it title
            setTitle(getString(R.string.dialog_title))
            // allow it to be cancellable
            setCancelable(false)
            // sets negative button configuration
            setNegativeButton("Cancel") { _, _ ->
                // cancels the generateGeodatabaseJob
                workManager.cancelAllWork()
            }
            // removes parent of the progressDialog layout, if previously assigned
            progressLayout.root.parent?.let { parent ->
                (parent as ViewGroup).removeAllViews()
            }
            // set the progressDialog Layout to this alert dialog
            setView(progressLayout.root)
        }.create()
    }

    /**
     * Request Post Notifications Permission for API level 33+.
     * https://developer.android.com/develop/ui/views/notifications/notification-permission
     */
    private fun requestNotificationPermission() {
        // request notification permission only for android versions >= 33
        if (Build.VERSION.SDK_INT >= 33) {
            // push notifications permission
            val permissionCheckPostNotifications =
                ContextCompat.checkSelfPermission(this@MainActivity, POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED

            // if permissions are not already granted, request permission from the user
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
        Log.e(TAG, message)
        Snackbar.make(mapView, message, Snackbar.LENGTH_SHORT).show()
    }
}
