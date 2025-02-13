package com.esri.arcgismaps.sample.displaydevicelocationwithfusedlocationdatasource.components

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.location.CustomLocationDataSource
import com.arcgismaps.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.time.Instant

class FusedLocationProvider(private val applicationContext: Context) : CustomLocationDataSource.LocationProvider {

    private val _headings = MutableSharedFlow<Double>()
    override val headings: Flow<Double> = _headings.asSharedFlow()

    private val _locations = MutableSharedFlow<Location>()
    override val locations: Flow<Location> = _locations.asSharedFlow()

    private var fusedLocationProviderClient: FusedLocationProviderClient? = null


    @SuppressLint("MissingPermission")
    fun startFusedLocationProvider() {
        val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(applicationContext).also {
            fusedLocationProviderClient = it
        }
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 0).build()

        val locationCallback by lazy {
            object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    locationResult.lastLocation?.let { fusedLocation ->
                        CoroutineScope(Dispatchers.IO).launch {
                            _locations.emit(
                                createArcGISLocationFromFusedLocation(
                                    fusedLocation = fusedLocation, isLastKnown = false
                                )
                            )
                            _headings.emit(fusedLocation.bearing.toDouble())
                        }
                    }
                }
            }
        }
        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest, locationCallback, Looper.getMainLooper()
        )
        /*
                fusedLocationProviderClient.lastLocation.addOnSuccessListener { fusedLocation ->
                    CoroutineScope(Dispatchers.IO).launch {
                        _locations.emit(
                            createArcGISLocationFromFusedLocation(fusedLocation = fusedLocation, isLastKnown = true)
                        )
                        _headings.emit(fusedLocation.bearing.toDouble())
                    }
                }*/
    }

    private fun createArcGISLocationFromFusedLocation(
        fusedLocation: android.location.Location, isLastKnown: Boolean
    ): Location {
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
            lastKnown = isLastKnown,
            timestamp = Instant.ofEpochMilli(fusedLocation.time),
            //additionalSourceProperties = fusedLocation.extras
        )
    }
}