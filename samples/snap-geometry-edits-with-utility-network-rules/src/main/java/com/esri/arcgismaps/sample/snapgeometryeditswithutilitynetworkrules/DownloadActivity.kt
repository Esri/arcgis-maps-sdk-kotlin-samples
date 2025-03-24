/*
 * COPYRIGHT 1995-2025 ESRI
 *
 * TRADE SECRETS: ESRI PROPRIETARY AND CONFIDENTIAL
 * Unpublished material - all rights reserved under the
 * Copyright Laws of the United States.
 *
 * For additional information, contact:
 * Environmental Systems Research Institute, Inc.
 * Attn: Contracts Dept
 * 380 New York Street
 * Redlands, California, USA 92373
 *
 * email: contracts@esri.com
 */
package com.esri.arcgismaps.sample.snapgeometryeditswithutilitynetworkrules

import android.content.Intent
import android.os.Bundle
import com.esri.arcgismaps.sample.sampleslib.DownloaderActivity

class DownloadActivity : DownloaderActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        downloadAndStartSample(
            Intent(this, MainActivity::class.java),
            // get the app name of the sample
            getString(R.string.snap_geometry_edits_with_utility_network_rules_app_name),
            listOf(
                // ArcGIS Portal item containing the mobile geodatabase containing data for the
                // Napperville gas utility network
                "https://www.arcgis.com/home/item.html?id=0fd3a39660d54c12b05d5f81f207dffd",
            )
        )
    }
}
