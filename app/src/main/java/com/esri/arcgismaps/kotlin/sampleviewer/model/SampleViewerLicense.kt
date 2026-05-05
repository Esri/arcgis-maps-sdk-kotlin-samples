/* Copyright 2026 Esri
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

package com.esri.arcgismaps.kotlin.sampleviewer.model

import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.LicenseKey
import com.esri.arcgismaps.kotlin.sampleviewer.BuildConfig

fun String.isUnsetKey(): Boolean {
    val trimmed = trim()
    return trimmed.isBlank() || trimmed.startsWith("DEFAULT")
}

fun lazySetArcGISLicense() {
    val standardLicenseKey = BuildConfig.ARCGIS_STANDARD_LICENSE_KEY
    if (standardLicenseKey.isUnsetKey()) return
    val standardKey = LicenseKey.create(standardLicenseKey) ?: return
    val extensionKeys = mutableListOf<LicenseKey>()
    val spatialAnalysisExtensionKey = BuildConfig.ARCGIS_SPATIAL_ANALYSIS_EXTENSION_KEY
    if (!spatialAnalysisExtensionKey.isUnsetKey()) {
        val spatialAnalysisKey = LicenseKey.create(spatialAnalysisExtensionKey) ?: return
        extensionKeys.add(spatialAnalysisKey)
    }
    val advancedEditingExtensionKey = BuildConfig.ARCGIS_ADVANCED_EDITING_EXTENSION_KEY
    if (!advancedEditingExtensionKey.isUnsetKey()) {
        val advancedEditingKey = LicenseKey.create(advancedEditingExtensionKey) ?: return
        extensionKeys.add(advancedEditingKey)
    }
    ArcGISEnvironment.setLicense(
        licenseKey = standardKey,
        LicenseExtensions = extensionKeys
    )
}
