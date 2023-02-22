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
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.work.*
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.Color
import com.arcgismaps.geometry.Envelope
import com.arcgismaps.geometry.Geometry
import com.arcgismaps.mapping.ArcGISMap
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
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

const val notificationIdParameter = "NotificationId"
const val jobParameter = "Job"


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
    private val progressDialog by lazy {
        GenerateOfflineMapDialogLayoutBinding.inflate(layoutInflater)
    }

    private val sharedPreferences by lazy {
        getSharedPreferences("JobState", MODE_PRIVATE)
    }

    private val dialog by lazy { createProgressDialog() }

    private val graphicsOverlay: GraphicsOverlay = GraphicsOverlay()

    private val downloadArea: Graphic = Graphic()

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
        mapView.map = map
        mapView.graphicsOverlays.add(graphicsOverlay)

        lifecycleScope.launch {
            map.load().onFailure {
                showError("Error loading map: ${it.message}")
                return@launch
            }

            // limit the map scale to the largest layer scale
            map.maxScale = map.operationalLayers[6].maxScale ?: 0.0
            map.minScale = map.operationalLayers[6].minScale ?: 0.0
            // add the graphics overlay to the map view when it is created
            // enable the take map offline button only after the map is loaded
            takeMapOfflineButton.isEnabled = true

            mapView.viewpointChanged.collect {
                // upper left corner of the area to take offline
                val minScreenPoint = ScreenCoordinate(200.0, 200.0)
                // lower right corner of the downloaded area
                val maxScreenPoint = ScreenCoordinate(
                    mapView.width - 200.0,
                    mapView.height - 200.0
                )
                // convert screen points to map points
                val minPoint = mapView.screenToLocation(minScreenPoint)
                val maxPoint = mapView.screenToLocation(maxScreenPoint)
                // use the points to define and return an envelope
                if (minPoint != null && maxPoint != null) {
                    val envelope = Envelope(minPoint, maxPoint)
                    downloadArea.geometry = envelope
                }
            }
        }

        takeMapOfflineButton.setOnClickListener {
            downloadArea.geometry?.let { geometry ->
                dialog.show()
                createOfflineMapJob(map, geometry)
                takeMapOfflineButton.isEnabled = false
            }
        }

        val jobId =
            UUID.nameUUIDFromBytes(sharedPreferences.getString("job", "").toString().toByteArray())
        observeWorkStatus(jobId)
    }

    private fun createOfflineMapJob(map: ArcGISMap, areaOfInterest: Geometry) {
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
        val offlineMapJob = offlineMapTask.createGenerateOfflineMapJob(
            generateOfflineMapParameters,
            offlineMapPath
        )

        File(offlineMapPath).deleteRecursively()
        startOfflineMapJob(offlineMapJob)
    }

    private fun startOfflineMapJob(offlineMapJob: GenerateOfflineMapJob) {
        val uniqueJobId = AtomicInteger(1).incrementAndGet()
        val jobJsonPath = getExternalFilesDir(null)?.path +
            getString(R.string.offlineJobJsonFile) + uniqueJobId


        val jobJsonFile = File(jobJsonPath)
        jobJsonFile.writeText(offlineMapJob.toJson())

        val workRequest =
            OneTimeWorkRequestBuilder<JobWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setInputData(workDataOf(notificationIdParameter to uniqueJobId,
                    jobParameter to jobJsonFile.absolutePath))
                .build()

        workManager.enqueueUniqueWork(uniqueJobId.toString(), ExistingWorkPolicy.KEEP, workRequest)
        sharedPreferences.edit().putString("job", uniqueJobId.toString()).apply()
        observeWorkStatus(workRequest.id)
    }

    private fun observeWorkStatus(jobId: UUID) {
        workManager.getWorkInfoByIdLiveData(jobId)
            .observe(this) { workInfo ->
                Log.d(TAG, "observeWorkStatus: $workInfo")
                when (workInfo.state) {
                    WorkInfo.State.SUCCEEDED -> {
                        dialog.dismiss()
                    }

                    WorkInfo.State.FAILED -> {
                        showError("Error generating offline map")
                    }

                    WorkInfo.State.RUNNING -> {
                        val progress = workInfo.progress
                        val value = progress.getInt("Progress", 0)
                        progressDialog.progressBar.progress = value
                        progressDialog.progressTextView.text = "$value%"
                    }
                    else -> {}
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
            progressDialog.root.parent?.let { parent ->
                (parent as ViewGroup).removeAllViews()
            }
            // set the progressDialog Layout to this alert dialog
            setView(progressDialog.root)
        }.create()
    }

    private fun showError(message: String) {
        Log.e(TAG, message)
        Snackbar.make(mapView, message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onStop() {
        super.onStop()
        dialog.dismiss()
    }
}
