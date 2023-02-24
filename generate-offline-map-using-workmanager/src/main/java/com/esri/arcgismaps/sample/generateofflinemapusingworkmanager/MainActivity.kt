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
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.asFlow
import androidx.work.WorkManager
import androidx.work.WorkInfo
import androidx.work.workDataOf
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.ExistingWorkPolicy
import androidx.work.OutOfQuotaPolicy
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

    // represents an Id for the unique worker so that
    // only one worker is active at a time
    private val uniqueWorkerId = "offlineJobWorker"

    // tag for the worker allowing us to query and observe
    // worker progress immediately or during later app launches
    private val jobWorkerRequestTag = "OfflineMapJob"

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

    private fun createOfflineMapJob(
        map: ArcGISMap,
        areaOfInterest: Geometry
    ): GenerateOfflineMapJob {
        File(offlineMapPath).deleteRecursively()

        val maxScale = map.maxScale
        val minScale = if (map.minScale <= maxScale) {
            maxScale + 1
        } else {
            map.minScale
        }

        val generateOfflineMapParameters = GenerateOfflineMapParameters(
            areaOfInterest,
            minScale,
            maxScale
        ).apply {
            continueOnErrors = false
        }

        val offlineMapTask = OfflineMapTask(map)
        return offlineMapTask.createGenerateOfflineMapJob(
            generateOfflineMapParameters,
            offlineMapPath
        )
    }

    private fun startOfflineMapJob(offlineMapJob: GenerateOfflineMapJob) {
        val offlineJobJsonPath = getExternalFilesDir(null)?.path +
            getString(R.string.offlineJobJsonFile)

        val offlineJobJsonFile = File(offlineJobJsonPath)
        offlineJobJsonFile.writeText(offlineMapJob.toJson())

        val notificationId = Random.Default.nextInt(1, 100)

        val workRequest =
            OneTimeWorkRequestBuilder<OfflineJobWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .addTag(jobWorkerRequestTag)
                .setInputData(
                    workDataOf(
                        notificationIdParameter to notificationId,
                        jobParameter to offlineJobJsonFile.absolutePath
                    )
                )
                .build()

        workManager.enqueueUniqueWork(uniqueWorkerId, ExistingWorkPolicy.REPLACE, workRequest)
    }

    /**
     * 
     */
    private fun observeWorkStatus() {
        val workInfoFlow = workManager.getWorkInfosByTagLiveData(jobWorkerRequestTag).asFlow()

        lifecycleScope.launch {
            workInfoFlow.collect { workInfoList ->
                Log.d(TAG, "observeWorkStatus: $workInfoList")
                if (workInfoList.size > 0) {
                    val workInfo = workInfoList[0]
                    // Log.d(TAG, "observeWorkStatus: $workInfo")
                    when (workInfo.state) {
                        WorkInfo.State.SUCCEEDED -> {
                            Log.d(TAG, "observeWorkStatus: Finished Job ${workInfo.state}")
                            if (progressDialog.isShowing) {
                                progressDialog.dismiss()
                            }
                            displayOfflineMap()
                        }

                        WorkInfo.State.FAILED -> {
                            showMessage("Error generating offline map")
                            if (progressDialog.isShowing) {
                                progressDialog.dismiss()
                            }
                            takeMapOfflineButton.isEnabled = true
                            workManager.pruneWork()
                        }

                        WorkInfo.State.RUNNING -> {
                            if (!progressDialog.isShowing) {
                                progressDialog.show()
                            }
                            val progress = workInfo.progress
                            val value = progress.getInt("Progress", 0)
                            progressLayout.progressBar.progress = value
                            progressLayout.progressTextView.text = "$value%"
                        }
                        else -> { /* other states are not relevant */ }
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
                // this removes the completed OfflineJobWorker WorkInfo from
                // the WorkManager's database. Otherwise, the observer will return the
                // WorkInfo on every launch until WorkManager auto-prunes
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
