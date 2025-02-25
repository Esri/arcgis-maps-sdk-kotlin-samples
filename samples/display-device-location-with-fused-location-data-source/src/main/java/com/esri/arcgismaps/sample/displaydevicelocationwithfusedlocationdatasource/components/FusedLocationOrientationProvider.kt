/* Copyright 2025 Esri
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

package com.esri.arcgismaps.sample.displaydevicelocationwithfusedlocationdatasource.components

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import android.util.Log
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.location.CustomLocationDataSource
import com.arcgismaps.location.Location
import com.google.android.gms.location.DeviceOrientation
import com.google.android.gms.location.DeviceOrientationListener
import com.google.android.gms.location.DeviceOrientationRequest
import com.google.android.gms.location.FusedOrientationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.concurrent.Executors

class FusedLocationOrientationProvider(applicationContext: Context) : CustomLocationDataSource.LocationProvider {

    private val _headings = MutableSharedFlow<Double>()
    // Note the override property here, required to implement the LocationProvider interface
    override val headings: Flow<Double> = _headings.asSharedFlow()

    private val _locations = MutableSharedFlow<Location>()
    // Note the override property here, required to implement the LocationProvider interface
    override val locations: Flow<Location> = _locations.asSharedFlow()

    // Set up fused location provider states
    private var fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(applicationContext)
    private var locationCallback: LocationCallback? = null
    private var emitLocationsJob: Job? = null
    private var priority: Int = Priority.PRIORITY_HIGH_ACCURACY
    private var intervalInSeconds: Long = 1L

    // Set up fused orientation provider states
    private var fusedOrientationProviderClient: FusedOrientationProviderClient =
        LocationServices.getFusedOrientationProviderClient(applicationContext)
    private var orientationListener: DeviceOrientationListener? = null
    private var emitHeadingsJob: Job? = null

    /**
     * Pass changes in priority to the fused location provider.
     */
    fun onPriorityChanged(priority: Int) {
        this.priority = priority
        startNewFusedLocationProvider(priority, intervalInSeconds)
    }

    /**
     * Pass changes in interval to the fused location provider.
     */
    fun onIntervalChanged(interval: Long) {
        this.intervalInSeconds = interval
        startNewFusedLocationProvider(priority, interval)
    }

    /**
     * Start the fused location and orientation providers.
     */
    fun start() {
        startNewFusedLocationProvider(priority, intervalInSeconds)
        startNewFusedOrientationProvider()

    }

    /**
     * Stop the fused location and orientation providers.
     */
    fun stop() {
        // Stop emitting locations into the locations flow
        emitLocationsJob?.cancel()
        locationCallback?.let { fusedLocationProviderClient.removeLocationUpdates(it) }
        // Stop emitting headings into the headings flow
        emitHeadingsJob?.cancel()
        orientationListener?.let { fusedOrientationProviderClient.removeOrientationUpdates(it) }
    }

    /**
     * Create a location request with the given priority and interval. Create a callback to receive location updates
     * and then request location updates with the location request and callback. In the callback, emit the location and
     * heading updates into the override flows in the LocationProvider interface.
     */
    @SuppressLint("MissingPermission") // Permission requests are handled in MainActivity
    private fun startNewFusedLocationProvider(
        priority: Int = Priority.PRIORITY_HIGH_ACCURACY,
        intervalInSeconds: Long = 1L
    ) {
        // Cancel any current jobs emitting into locations
        emitLocationsJob?.cancel()

        // Clear any previous location updates
        locationCallback?.let {
            fusedLocationProviderClient.removeLocationUpdates(it)
        }

        // Create a location request with the desired priority and interval
        val locationRequest = LocationRequest.Builder(priority, intervalInSeconds * 1000).build()

        // Create a new location callback to emit location updates
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { fusedLocation ->
                    emitLocationsJob = CoroutineScope(Dispatchers.IO).launch {
                        // Emit the ArcGIS location object into the Location Provider's overridden locations flow
                        _locations.emit(
                            createArcGISLocationFromFusedLocation(
                                fusedLocation = fusedLocation
                            )
                        )
                    }
                }
            }
        }
        // Requests location updates with the given request and results delivered to the given listener on the specified
        // Looper
        locationCallback?.let { locationCallback ->
            fusedLocationProviderClient.requestLocationUpdates(
                locationRequest, locationCallback, Looper.getMainLooper()
            )
        }
    }

    /**
     * Create a new fused orientation provider and request orientation updates. Emit the heading updates into the
     * override flow in the LocationProvider interface.
     */
    private fun startNewFusedOrientationProvider() {
        // Cancel any current jobs emitting into locations
        emitHeadingsJob?.cancel()

        // Create an FOP listener
        orientationListener = DeviceOrientationListener { orientation: DeviceOrientation ->
            emitHeadingsJob = CoroutineScope(Dispatchers.IO).launch {
                // Emit the fused orientation's heading into the Location Provider's overridden headings flow
                _headings.emit(orientation.headingDegrees.toDouble())
            }
        }

        // Create a new orientation request with the default request period. 
        // Other DeviceOrientationRequest than OUTPUT_PERIOD_DEFAULT can be defined here.
        val orientationRequest =
            DeviceOrientationRequest.Builder(DeviceOrientationRequest.OUTPUT_PERIOD_DEFAULT).build()

        // Register the request and listener
        orientationListener?.let {
            fusedOrientationProviderClient
                .requestOrientationUpdates(orientationRequest, Executors.newSingleThreadExecutor(), it)
                .addOnSuccessListener {
                    Log.i(
                        FusedLocationOrientationProvider::class.simpleName,
                        "Registration Success"
                    )
                }
                .addOnFailureListener { error ->
                    Log.e(
                        FusedLocationOrientationProvider::class.simpleName,
                        "Registration Failure: " + error.message
                    )
                }
        }
    }
}

/**
 * Creates an ArcGIS Maps SDK Location object from a Fused Location object.
 */
private fun createArcGISLocationFromFusedLocation(fusedLocation: android.location.Location): Location {
    return Location.create(
        position = Point(
            x = fusedLocation.longitude,
            y = fusedLocation.latitude,
            z = fusedLocation.altitude,
            SpatialReference.wgs84()
        ),
        horizontalAccuracy = fusedLocation.accuracy.toDouble(),
        verticalAccuracy = fusedLocation.verticalAccuracyMeters.toDouble(),
        speed = fusedLocation.speed.toDouble(),
        course = fusedLocation.bearing.toDouble(),
        // If the timestamp is more than 5 seconds old, set lastKnown to true
        lastKnown = (Instant.now().toEpochMilli() - fusedLocation.time) > 5000,
        timestamp = Instant.ofEpochMilli(fusedLocation.time),
    )
}
