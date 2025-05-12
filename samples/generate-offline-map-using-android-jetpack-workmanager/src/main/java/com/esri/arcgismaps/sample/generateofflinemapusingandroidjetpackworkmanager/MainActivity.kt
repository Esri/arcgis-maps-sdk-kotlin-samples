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
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.MobileMapPackage
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.portal.Portal
import com.arcgismaps.tasks.offlinemaptask.DownloadPreplannedOfflineMapJob
import com.arcgismaps.tasks.offlinemaptask.OfflineMapTask
import com.arcgismaps.tasks.offlinemaptask.PreplannedMapArea
import com.arcgismaps.tasks.offlinemaptask.PreplannedUpdateMode
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
        DataBindingUtil.setContentView(
            this,
            R.layout.generate_offline_map_using_android_jetpack_workmanager_activity_main
        )
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

    // create a portal item with the itemId of the web map
    val portal = Portal("https://www.arcgis.com")
    val portalItem = PortalItem(portal, "acc027394bc84c2fb04d1ed317aac674")

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
            mapView.graphicsOverlays.clear()

            // set up the portal item to take offline
            setUpMapView()
        }
    }

    /**
     * Sets up a portal item and displays map area to take offline
     */
    private fun setUpMapView() {

        // create and add a map with with portal item
        val map = ArcGISMap(portalItem)
        // apply mapview assignments
        mapView.apply {
            this.map = map
        }

        lifecycleScope.launch {
            map.load().onFailure {
                // show an error and return if the map load failed
                showMessage("Error loading map: ${it.message}")
                return@launch
            }
            // enable the take map offline button only after the map is loaded
            takeMapOfflineButton.isEnabled = true

        }

        // set onclick listener for the takeMapOfflineButton
        takeMapOfflineButton.setOnClickListener {
            lifecycleScope.launch {
                // create an OfflineMapJob
                val downloadPreplannedOfflineMapJob = createOfflineMapJob(portalItem)
                // start the OfflineMapJob
                startOfflineMapJob(downloadPreplannedOfflineMapJob)
                // show the progress dialog
                progressDialog.show()
                // disable the button
                takeMapOfflineButton.isEnabled = false
            }
        }

        // start observing the worker's progress and status
        observeWorkStatus()
    }

    private suspend fun createOfflineMapJob(
        portalItem: PortalItem
    ): DownloadPreplannedOfflineMapJob {
        // check and delete if the offline map package file already exists
        File(offlineMapPath).deleteRecursively()

        // A list of preplanned map areas populated by the offline map task
        val preplannedMapAreas = mutableListOf<PreplannedMapArea>()

        val offlineMapTask = OfflineMapTask(portalItem).apply {
            val list = getPreplannedMapAreas().getOrNull()?.toList() ?: emptyList()
            preplannedMapAreas.addAll(list)
        }

        val preplannedMapArea = preplannedMapAreas.first()

        // Create default download parameters from the offline map task
        val params =
            offlineMapTask.createDefaultDownloadPreplannedOfflineMapParameters(preplannedMapArea)
                .getOrThrow()

        // Set the update mode to receive no updates
        params.updateMode = PreplannedUpdateMode.NoUpdates
        params.continueOnErrors = false
        // Define the path where the map will be saved
        val downloadDirectoryPath = offlineMapPath + File.separator + portalItem.itemId
        File(downloadDirectoryPath).mkdirs()
        // Create a job to download the preplanned offline map
        val downloadPreplannedOfflineMapJob =
            offlineMapTask.createDownloadPreplannedOfflineMapJob(
                parameters = params,
                downloadDirectoryPath = downloadDirectoryPath
            )
        return downloadPreplannedOfflineMapJob
    }

    /**
     * Starts the [downloadPreplannedOfflineMapJob] using OfflineJobWorker with WorkManager. The [downloadPreplannedOfflineMapJob] is
     * serialized into a json file and the uri is passed to the OfflineJobWorker, since WorkManager
     * enforces a MAX_DATA_BYTES for the WorkRequest's data
     */
    private fun startOfflineMapJob(downloadPreplannedOfflineMapJob: DownloadPreplannedOfflineMapJob) {
        // create a temporary file path to save the offlineMapJob json file
        val offlineJobJsonPath = getExternalFilesDir(null)?.path +
                getString(R.string.offlineJobJsonFile)

        // create the json file
        val offlineJobJsonFile = File(offlineJobJsonPath)
        // serialize the offlineMapJob into the file
        offlineJobJsonFile.writeText(downloadPreplannedOfflineMapJob.toJson())

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
            val offlineMapPath = offlineMapPath + File.separator + portalItem.itemId
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
            setTitle("Downloading preplanned area...")
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
