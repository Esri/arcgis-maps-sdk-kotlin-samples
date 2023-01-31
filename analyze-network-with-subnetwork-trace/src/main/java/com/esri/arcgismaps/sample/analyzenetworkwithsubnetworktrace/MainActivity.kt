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

package com.esri.arcgismaps.sample.analyzenetworkwithsubnetworktrace

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.RelativeLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.data.CodedValue
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.utilitynetworks.UtilityAttributeComparisonOperator
import com.arcgismaps.utilitynetworks.UtilityElement
import com.arcgismaps.utilitynetworks.UtilityNetwork
import com.arcgismaps.utilitynetworks.UtilityNetworkAttribute
import com.arcgismaps.utilitynetworks.UtilityTier
import com.arcgismaps.utilitynetworks.UtilityTraceConditionalExpression
import com.esri.arcgismaps.sample.analyzenetworkwithsubnetworktrace.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.java.simpleName

    // set up data binding for the activity
    private val activityMainBinding: ActivityMainBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.activity_main)
    }

    private val exampleTextView: TextView by lazy {
        activityMainBinding.exampleTextView
    }

    private val sourceSpinner: Spinner by lazy {
        activityMainBinding.sourceSpinner
    }

    private val operatorSpinner: Spinner by lazy {
        activityMainBinding.operatorSpinner
    }

    private val expressionTextView: TextView by lazy {
        activityMainBinding.expressionTextView
    }

    private val valuesSpinner: Spinner by lazy {
        activityMainBinding.valuesSpinner
    }

    private val valuesBackgroundView: RelativeLayout by lazy {
        activityMainBinding.valuesBackgroundView
    }

    private val valueBooleanButton: ToggleButton by lazy {
        activityMainBinding.valueBooleanButton
    }

    private val valuesEditText: EditText by lazy {
        activityMainBinding.valuesEditText
    }

    private val utilityNetwork by lazy {
        UtilityNetwork("https://sampleserver7.arcgisonline.com/server/rest/services/UtilityNetwork/NapervilleElectric/FeatureServer").apply {
            // set user credentials to authenticate with the service
            // NOTE: a licensed user is required to perform utility network operations
            // credential = UserCredential("viewer01", "I68VGU^nMurF")
        }
    }

    private var initialExpression: UtilityTraceConditionalExpression? = null
    private var sourceTier: UtilityTier? = null
    private var sources: List<UtilityNetworkAttribute>? = null
    private var operators: Array<UtilityAttributeComparisonOperator>? = null
    private var startingLocation: UtilityElement? = null
    private var values: List<CodedValue>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ArcGISEnvironment.applicationContext = this

        val authenticationHandler = AuthenticationChallengeHandler(this, authenticator,
            tokenUsername,
            tokenPassword)

        exampleTextView.movementMethod = ScrollingMovementMethod()

        // create a utility network and wait for it to finish to load
        lifecycleScope.launch {
            utilityNetwork.load().getOrElse {
                return@launch showError("Error loading utility network:" + it.message.toString())
            }

            // create a list of utility network attributes whose system is not defined
            sources = utilityNetwork.definition?.networkAttributes?.filter { !it.isSystemDefined }
            sourceSpinner.apply {
                // assign an adapter to the spinner with source names
                adapter = sources?.let { utilityNetworkAttributes ->
                    ArrayAdapter(
                        applicationContext,
                        android.R.layout.simple_list_item_1,
                        utilityNetworkAttributes.map { it.name })
                }

                // add an on item selected listener which calls on comparison source changed
                onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        (sources?.get(sourceSpinner.selectedItemPosition))
                        onComparisonSourceChanged(sources?.get(sourceSpinner.selectedItemPosition)!!)
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
            }

            // create a list of utility attribute comparison operators
        }
    }

    /**
     * When a comparison source attribute is chosen check if it's a coded value domain and, if it is,
     * present a spinner of coded value domains. If not, show the correct UI view for the utility
     * network attribute data type.
     *
     * @param attribute being compared
     */
    private fun onComparisonSourceChanged(attribute: UtilityNetworkAttribute) {

    }

    private fun showError(message: String) {
        Log.e(TAG, message)
        Snackbar.make(activityMainBinding.root, message, Snackbar.LENGTH_SHORT).show()
    }
}
