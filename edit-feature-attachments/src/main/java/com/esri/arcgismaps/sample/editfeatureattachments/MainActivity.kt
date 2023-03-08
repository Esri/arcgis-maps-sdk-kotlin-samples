/* Copyright 2023 Esri
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

package com.esri.arcgismaps.sample.editfeatureattachments

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Point
import android.os.Bundle
import android.util.Log
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.data.Attachment
import com.arcgismaps.data.FeatureRequestMode
import com.arcgismaps.data.ServiceFeatureTable
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.GeoElement
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.FeatureLayer
import com.esri.arcgismaps.sample.editfeatureattachments.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.java.simpleName

    // set up data binding for the activity
    private val activityMainBinding: ActivityMainBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.activity_main)
    }

    private val mapView by lazy {
        activityMainBinding.mapView
    }

    private val REQUEST_CODE = 100
    private val mCalloutLayout: RelativeLayout? = null

    private var mFeatureLayer: FeatureLayer? = null
    private var mSelectedArcGISFeature: ArcGISFeature? = null
    private val mTapPoint: Point? = null

    private var attachments: List<Attachment>? = null
    private var damageType: String? = null
    private var mAttributeID: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)
        lifecycle.addObserver(mapView)

        // create and add a map with a navigation night basemap style
        val map = ArcGISMap(BasemapStyle.ArcGISStreets)
        mapView.map = map
        mapView.setViewpoint(Viewpoint(40.0, -95.0, 1e7))

        // create feature layer with its service feature table and create the service feature table
        val mServiceFeatureTable = ServiceFeatureTable(getString(R.string.sample_service_url))
        mServiceFeatureTable.featureRequestMode = FeatureRequestMode.OnInteractionCache
        // create the feature layer using the service feature table
        mFeatureLayer = FeatureLayer(mServiceFeatureTable)
        // add the layer to the map
        map.operationalLayers.add(mFeatureLayer!!)

        // TODO get callout, set content and show
        //mCallout = mMapView.getCallout()

        lifecycleScope.launch {
            mapView.onSingleTapConfirmed.collect { tapConfirmedEvent ->
                val mapPoint = tapConfirmedEvent.mapPoint
                val screenCoordinate = tapConfirmedEvent.screenCoordinate

                // clear any previous selection
                mFeatureLayer!!.clearSelection()
                mSelectedArcGISFeature = null
                //TODO callout.dismiss
                mapView.identifyLayer(
                    layer = mFeatureLayer!!,
                    screenCoordinate = screenCoordinate,
                    tolerance = 5.0,
                    returnPopupsOnly = false,
                    maximumResults = 1
                ).onSuccess { layerResult ->
                    val resultGeoElements: List<GeoElement> = layerResult.geoElements
                    if (resultGeoElements.isNotEmpty() && resultGeoElements[0] is ArcGISFeature) {
                        // retrieve and set the currently selected feature
                        mSelectedArcGISFeature = resultGeoElements[0] as ArcGISFeature
                        // highlight the currently selected feature
                        mFeatureLayer!!.selectFeature(mSelectedArcGISFeature!!)
                        mAttributeID = mSelectedArcGISFeature?.attributes?.get("objectid").toString()
                        // get the number of attachments
                        attachments = mSelectedArcGISFeature?.fetchAttachments()?.getOrThrow()
                        // show callout with the value for the attribute "typdamage" of the selected feature
                        damageType = mSelectedArcGISFeature?.attributes?.get("typdamage").toString()
                        showDialog(damageType!!, attachments!!.size)
                        //showCallout(mSelectedArcGISFeatureAttributeValue, attachments!!.size)
                    } else {
                        // none of the features on the map were selected
                        // TODO
                        // mCallout.dismiss();
                    }
                }.onFailure {
                    showError("Failed to select feature: ${it.message}")
                }


            }
        }

    }

    private fun showDialog(damageType: String, attachmentSize: Int) {
        val dialogBuilder = AlertDialog.Builder(this).apply {
            setTitle("Damage type: $damageType")
            setMessage(getString(R.string.attachment_info_message) + attachmentSize)
            setNegativeButton("Dismiss") { _, _ ->

            }
            setPositiveButton("Edit attachments") { _, _ ->
                // start EditAttachmentActivity to view/edit the attachments
                val myIntent = Intent(this@MainActivity, EditAttachmentActivity::class.java)
                myIntent.putExtra(getString(R.string.attribute), mAttributeID)
                myIntent.putExtra(getString(R.string.noOfAttachments), attachments!!.size)
                val bundle = Bundle()
                startActivityForResult(
                    myIntent,
                    REQUEST_CODE,
                    bundle
                )
            }
        }
        val dialog = dialogBuilder.create()
        dialog.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Check which request we're responding to
        if (requestCode == REQUEST_CODE) {
            val noOfAttachments =
                data?.extras!!.getInt(application.getString(R.string.noOfAttachments))
            // update the callout with attachment count
            showDialog(damageType!!, noOfAttachments)
        }
    }


    private fun showError(message: String) {
        Log.e(TAG, message)
        Snackbar.make(mapView, message, Snackbar.LENGTH_SHORT).show()
    }
}
