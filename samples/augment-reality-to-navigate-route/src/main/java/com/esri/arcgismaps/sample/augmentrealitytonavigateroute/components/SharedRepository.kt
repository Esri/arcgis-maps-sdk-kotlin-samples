package com.esri.arcgismaps.sample.augmentrealitytonavigateroute.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.arcgismaps.tasks.networkanalysis.RouteResult

object SharedRepository {

    private var _Route by mutableStateOf<RouteResult?>(null)
    val route
        get() = _Route

    fun updateRoute(route: RouteResult?) {
        _Route = route
    }
}
