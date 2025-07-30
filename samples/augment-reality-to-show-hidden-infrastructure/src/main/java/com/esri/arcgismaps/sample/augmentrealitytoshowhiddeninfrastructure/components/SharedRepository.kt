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

package com.esri.arcgismaps.sample.augmentrealitytoshowhiddeninfrastructure.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Shared repository to hold the route result generated in the route view model and passed to the augmented reality view
 * model.
 */
object SharedRepository {

    private var _pipeInfoList: MutableList<PipeInfo> = mutableListOf()
    val pipeInfoList
        get() = _pipeInfoList

    private var _hasNonDefaultAPIKey by mutableStateOf(false)
    val hasNonDefaultAPIKey: Boolean
        get() = _hasNonDefaultAPIKey

    fun updateHasNonDefaultAPIKey(hasNonDefaultAPIKey: Boolean) {
        _hasNonDefaultAPIKey = hasNonDefaultAPIKey
    }
}


