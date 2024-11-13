/* Copyright 2024 Esri
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

package com.esri.arcgismaps.sample.geoviewnperepro.components

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.AndroidViewModel
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.symbology.PictureMarkerSymbol
import com.esri.arcgismaps.sample.geoviewnperepro.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MapViewModel(application: Application) : AndroidViewModel(application) {
    private val resources = application.resources
    val pms = PictureMarkerSymbol.createWithImage(
        BitmapFactory.decodeResource(resources, R.drawable.pin).toDrawable(resources)
    )

    val startPoint = Point(
        x = -0.284606,
        y = 36.082152,
        spatialReference = SpatialReference.wgs84()
    )
    val endPoint = Point(
        x = -0.350597,
        y = 36.181794,
        spatialReference = SpatialReference.wgs84()
    )

    // cached bitmap to replace MapViews in column
    private val _cacheMapImageStateFlow = MutableStateFlow<Map<Long, Bitmap?>>(emptyMap())
    val cacheMapImageStateFlow = _cacheMapImageStateFlow.asStateFlow()

    fun cacheMapImage(tripId: Long, image: Bitmap) {
        _cacheMapImageStateFlow.update {
            it.toMutableMap().apply {
                put(tripId, image)
            }
        }
    }
}
