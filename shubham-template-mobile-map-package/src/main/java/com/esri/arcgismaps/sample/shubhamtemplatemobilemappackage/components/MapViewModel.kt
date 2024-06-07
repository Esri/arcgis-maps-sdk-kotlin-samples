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

package com.esri.arcgismaps.sample.shubhamtemplatemobilemappackage.components

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import com.arcgismaps.mapping.Viewpoint

class MapViewModel(application: Application) : AndroidViewModel(application) {
    private val viewpointAmerica = Viewpoint(39.8, -98.6, 10e7)
    private val viewpointAsia = Viewpoint(39.8, 98.6, 10e7)
    var viewpoint =  mutableStateOf(viewpointAmerica)
    /**
     * Switch between two basemaps
     */
    fun changeBasemap() {
        viewpoint.value =
            if (viewpoint.value == viewpointAmerica) viewpointAsia else viewpointAmerica
    }
}
