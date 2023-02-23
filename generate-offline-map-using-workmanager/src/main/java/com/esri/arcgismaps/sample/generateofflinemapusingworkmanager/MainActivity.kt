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

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.asFlow
import androidx.work.WorkManager
import androidx.work.WorkInfo
import androidx.work.workDataOf
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
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
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

const val notificationIdParameter = "NotificationId"
const val jobParameter = "Job"
const val jobTag = "OfflineMapJob"


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

    private val workManager by lazy {
        WorkManager.getInstance(this)
    }

    private val offlineMapPath by lazy {
        getExternalFilesDir(null)?.path + getString(R.string.offlineMapFile)
    }

    // shows the geodatabase loading progress
    private val progressLayout by lazy {
        GenerateOfflineMapDialogLayoutBinding.inflate(layoutInflater)
    }

    private val progressDialog by lazy {
        createProgressDialog()
    }

    private val graphicsOverlay = GraphicsOverlay()

    private val downloadArea = Graphic()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mapView.keepScreenOn = true

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
        mapView.apply {
            this.map = map
            graphicsOverlays.add(graphicsOverlay)
        }

        lifecycleScope.launch {
            map.load().onFailure {
                showError("Error loading map: ${it.message}")
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

        takeMapOfflineButton.setOnClickListener {
            downloadArea.geometry?.let { geometry ->
                val offlineMapJob = createOfflineMapJob(map, geometry)
                startOfflineMapJob(offlineMapJob)
                progressDialog.show()
                takeMapOfflineButton.isEnabled = false
            }
        }

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
        )
        generateOfflineMapParameters.continueOnErrors = false

        val offlineMapTask = OfflineMapTask(map)
        return offlineMapTask.createGenerateOfflineMapJob(
            generateOfflineMapParameters,
            offlineMapPath
        )
    }

    private fun startOfflineMapJob(offlineMapJob: GenerateOfflineMapJob) {
        val uniqueJobId = AtomicInteger(1).incrementAndGet()
        val jobJsonPath = getExternalFilesDir(null)?.path +
            getString(R.string.offlineJobJsonFile) + uniqueJobId

        val jobJsonFile = File(jobJsonPath)
        jobJsonFile.writeText(offlineMapJob.toJson())

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresStorageNotLow(true)
            .build()

        val workRequest =
            OneTimeWorkRequestBuilder<JobWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .addTag(jobTag)
                .setConstraints(constraints)
                .setInputData(
                    workDataOf(
                        notificationIdParameter to uniqueJobId,
                        jobParameter to jobJsonFile.absolutePath
                    )
                )
                .build()

        workManager.enqueueUniqueWork(uniqueJobId.toString(), ExistingWorkPolicy.KEEP, workRequest)
    }

    private fun observeWorkStatus() {
        val workInfoFlow = workManager.getWorkInfosByTagLiveData(jobTag).asFlow()

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
                            showError("Error generating offline map")
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
                        else -> { /* */
                        }
                    }
                }
            }
        }
    }

    private fun displayOfflineMap() {
        lifecycleScope.launch {
            if (File(offlineMapPath).exists()) {
                val mapPackage = MobileMapPackage(offlineMapPath)
                mapPackage.load().onFailure {
                    showError("Error loading map package: ${it.message}")
                    return@launch
                }

                val map = mapPackage.maps.first()
                map.load().onFailure {
                    showError("Error loading map: ${it.message}")
                    return@launch
                }

                mapView.map = map
                workManager.pruneWork()
                Toast.makeText(this@MainActivity, "Loaded offline map", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

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

    private fun showError(message: String) {
        Log.e(TAG, message)
        Snackbar.make(mapView, message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        progressDialog.dismiss()
    }
}
