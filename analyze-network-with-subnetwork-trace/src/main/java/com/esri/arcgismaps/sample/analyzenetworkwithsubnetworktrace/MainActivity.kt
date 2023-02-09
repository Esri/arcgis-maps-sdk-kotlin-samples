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
import android.text.InputType
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.RelativeLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.Guid
import com.arcgismaps.LoadStatus
import com.arcgismaps.data.CodedValue
import com.arcgismaps.data.CodedValueDomain
import com.arcgismaps.httpcore.authentication.ArcGISAuthenticationChallengeHandler
import com.arcgismaps.httpcore.authentication.ArcGISAuthenticationChallengeResponse
import com.arcgismaps.httpcore.authentication.TokenCredential
import com.arcgismaps.utilitynetworks.UtilityAttributeComparisonOperator
import com.arcgismaps.utilitynetworks.UtilityCategoryComparison
import com.arcgismaps.utilitynetworks.UtilityElement
import com.arcgismaps.utilitynetworks.UtilityElementTraceResult
import com.arcgismaps.utilitynetworks.UtilityNetwork
import com.arcgismaps.utilitynetworks.UtilityNetworkAttribute
import com.arcgismaps.utilitynetworks.UtilityNetworkAttributeComparison
import com.arcgismaps.utilitynetworks.UtilityNetworkAttributeDataType
import com.arcgismaps.utilitynetworks.UtilityTier
import com.arcgismaps.utilitynetworks.UtilityTraceAndCondition
import com.arcgismaps.utilitynetworks.UtilityTraceConditionalExpression
import com.arcgismaps.utilitynetworks.UtilityTraceConfiguration
import com.arcgismaps.utilitynetworks.UtilityTraceOrCondition
import com.arcgismaps.utilitynetworks.UtilityTraceParameters
import com.arcgismaps.utilitynetworks.UtilityTraceType
import com.arcgismaps.utilitynetworks.UtilityTraversability
import com.esri.arcgismaps.sample.analyzenetworkwithsubnetworktrace.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

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
        UtilityNetwork(getString(R.string.utility_network_url))
    }

    private var initialExpression: UtilityTraceConditionalExpression? = null
    private var sourceTier: UtilityTier? = null
    private var utilityTraceConfiguration: UtilityTraceConfiguration? = null
    private var sources: List<UtilityNetworkAttribute>? = null
    private var operators: Array<UtilityAttributeComparisonOperator>? = null
    private var startingLocation: UtilityElement? = null
    private var values: List<CodedValue>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ArcGISEnvironment.applicationContext = this
        ArcGISEnvironment.authenticationManager.arcGISAuthenticationChallengeHandler =
            ArcGISAuthenticationChallengeHandler { challenge ->
                val result: Result<TokenCredential> = runBlocking {
                    TokenCredential.create(challenge.requestUrl, "viewer01", "I68VGU^nMurF", 0)
                }
                if (result.getOrNull() != null) {
                    val credential = result.getOrNull()
                    return@ArcGISAuthenticationChallengeHandler ArcGISAuthenticationChallengeResponse
                        .ContinueWithCredential(credential!!)
                } else {
                    val ex = result.exceptionOrNull()
                    return@ArcGISAuthenticationChallengeHandler ArcGISAuthenticationChallengeResponse
                        .ContinueAndFailWithError(ex!!)
                }
            }

        exampleTextView.movementMethod = ScrollingMovementMethod()

        // create a utility network and wait for it to finish to load
        lifecycleScope.launch {
            utilityNetwork.load().getOrElse {
                return@launch showError("Error loading utility network: " + it.message.toString())
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
                        id: Long,
                    ) {
                        (sources?.get(sourceSpinner.selectedItemPosition))
                        onComparisonSourceChanged(sources?.get(sourceSpinner.selectedItemPosition)!!)
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
            }

            // create a list of utility attribute comparison operators
            operators =
                UtilityAttributeComparisonOperator::class.sealedSubclasses.mapNotNull { it.objectInstance }
                    .toTypedArray().also { operators ->
                        // assign operator spinner an adapter of operator names
                        operatorSpinner.adapter = ArrayAdapter(
                            applicationContext,
                            android.R.layout.simple_list_item_1,
                            operators.map { it::class.simpleName })
                    }

            // create a default starting location
            val networkSource =
                utilityNetwork.definition?.getNetworkSource("Electric Distribution Device")
                    ?: return@launch
            val assetGroup = networkSource.getAssetGroup("Circuit Breaker") ?: return@launch
            val assetType = assetGroup.getAssetType("Three Phase") ?: return@launch
            val globalId = Guid("1CAF7740-0BF4-4113-8DB2-654E18800028")
            val terminal = assetType.terminalConfiguration?.terminals?.first { it.name == "Load" }
                ?: return@launch
            // utility element to start the trace from
            startingLocation = utilityNetwork.createElement(assetType, globalId, terminal)

            // get a default trace configuration from a tier to update the UI
            val domainNetwork =
                utilityNetwork.definition?.getDomainNetwork("ElectricDistribution") ?: return@launch
            sourceTier = domainNetwork.getTier("Medium Voltage Radial")?.apply {
                utilityTraceConfiguration = getDefaultTraceConfiguration()?.apply {
                    (traversability?.barriers as? UtilityTraceConditionalExpression)?.let {
                        expressionTextView.text = expressionToString(it)
                        initialExpression = it
                    }
                }
            }
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
        // if the domain is a coded value domain
        (attribute.domain as? CodedValueDomain)?.let { codedValueDomain ->
            // update the list of coded values
            values = codedValueDomain.codedValues
            // show the values spinner
            setVisible(valuesBackgroundView.id)
            // update the values spinner adapter
            valuesSpinner.adapter = ArrayAdapter(
                applicationContext,
                android.R.layout.simple_list_item_1,
                // add the coded values from the coded value domain to the values spinner
                codedValueDomain.codedValues.map { it.name }
            )
            // if the domain is not a coded value domain
        } ?: when (attribute.dataType) {
            UtilityNetworkAttributeDataType.Boolean -> {
                setVisible(valueBooleanButton.id)
            }
            UtilityNetworkAttributeDataType.Double, UtilityNetworkAttributeDataType.Float -> {
                // show the edit text and only allow numbers (decimals allowed)
                valuesEditText.inputType =
                    InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                setVisible(valuesEditText.id)
            }
            UtilityNetworkAttributeDataType.Integer -> {
                // show the edit text only allowing for integer input
                valuesEditText.inputType = InputType.TYPE_CLASS_NUMBER
                setVisible(valuesEditText.id)
            }
            else -> {
                showError("Unexpected utility network attribute data type.")
            }
        }
    }

    /**
     * Add a new barrier condition to the trace options.
     *
     * @param view of the add button
     */
    fun addCondition(view: View) {
        // if source tier doesn't contain a trace configuration, create one
        val traceConfiguration = utilityTraceConfiguration?.apply {
            // if the trace configuration doesn't contain traversability, create one
            traversability = UtilityTraversability()
        } ?: return

        // get the currently selected attribute
        val attribute = sources?.get(sourceSpinner.selectedItemPosition)
        attribute?.let {
            // get the currently selected attribute operator
            val attributeOperator = operators?.get(operatorSpinner.selectedItemPosition)
            attributeOperator?.let {
                // if the other value is a coded value domain
                val otherValue = if (attribute.domain is CodedValueDomain) {
                    values?.get(valuesSpinner.selectedItemPosition)?.code?.let {
                        convertToDataType(it, attribute.dataType)
                    }
                } else {
                    convertToDataType(valuesEditText.text, attribute.dataType)
                }

                if (otherValue == null) {
                    return
                }

                // NOTE: You may also create a UtilityNetworkAttributeComparison with another
                // NetworkAttribute
                var expression: UtilityTraceConditionalExpression =
                    UtilityNetworkAttributeComparison(
                        attribute,
                        attributeOperator,
                        otherValue.toString()
                    )
                (traceConfiguration.traversability?.barriers as? UtilityTraceConditionalExpression)?.let { otherExpression ->
                    // NOTE: You may also combine expressions with UtilityTraceAndCondition
                    expression = UtilityTraceOrCondition(otherExpression, expression)
                }
                traceConfiguration.traversability?.barriers = expression
                expressionTextView.text = expressionToString(expression)

                /*
               val error =
                   "Error creating UtilityNetworkAttributeComparison! Did you forget to input a numeric value? ${e.message}"
               Log.e(TAG, error)
               Toast.makeText(this@MainActivity, error, Toast.LENGTH_LONG).show()
               return
                 */

            }
        }
    }

    /**
     * Show the given UI view and hide the others which share the same space.
     *
     * @param id of the view to make visible
     */
    private fun setVisible(id: Int) {
        when (id) {
            valuesBackgroundView.id -> {
                valuesBackgroundView.visibility = View.VISIBLE
                valueBooleanButton.visibility = View.GONE
                valuesEditText.visibility = View.GONE
            }
            valuesEditText.id -> {
                valuesEditText.visibility = View.VISIBLE
                valueBooleanButton.visibility = View.GONE
                valuesBackgroundView.visibility = View.GONE
            }
            valueBooleanButton.id -> {
                valueBooleanButton.visibility = View.VISIBLE
                valuesBackgroundView.visibility = View.GONE
                valuesEditText.visibility = View.GONE
            }
        }
    }

    /**
     * Run the network trace with the parameters and display the result in an alert dialog.
     *
     * @param view of the trace button
     */
    fun trace(view: View) {
        // don't attempt a trace on an unloaded utility network
        if (utilityNetwork.loadStatus.value != LoadStatus.Loaded) {
            return
        }
        val parameters = UtilityTraceParameters(
            UtilityTraceType.Subnetwork,
            listOf(startingLocation).requireNoNulls()
        )

        lifecycleScope.launch {
            val utilityTraceResults = utilityNetwork.trace(parameters).getOrElse {
                return@launch showError(
                    "For a working barrier condition, try \"Transformer Load\" Equal \"15\": " +
                            it.message.toString()
                )
            }

            val elementTraceResult = utilityTraceResults.first() as UtilityElementTraceResult
            // create an alert dialog
            AlertDialog.Builder(this@MainActivity).apply {
                // set the alert dialog title
                setTitle("Trace result")
                // show the element result count
                setMessage(elementTraceResult.elements.count().toString() + " elements found.")
            }.show()
        }
    }

    /**
     * Convert the given UtilityTraceConditionalExpression into a string.
     *
     * @param expression to convert to a string
     */
    private fun expressionToString(expression: UtilityTraceConditionalExpression): String? {
        when (expression) {
            // when the expression is a category comparison expression
            is UtilityCategoryComparison -> {
                return expression.category.name + " " + expression.comparisonOperator
            }
            // when the expression is an attribute comparison expression
            is UtilityNetworkAttributeComparison -> {
                // the name and comparison operator of the expression
                val networkAttributeNameAndOperator =
                    expression.networkAttribute.name + " " + expression.comparisonOperator + " "
                // check whether the network attribute has a coded value domain
                (expression.networkAttribute.domain as? CodedValueDomain)?.let { codedValueDomain ->
                    // if there's a coded value domain name
                    val codedValueDomainName = codedValueDomain.codedValues.first { codedValue ->
                        codedValue.code?.let { code ->
                            convertToDataType(code,
                                expression.networkAttribute.dataType)
                        } ==
                                expression.value?.let { value ->
                                    convertToDataType(
                                        value,
                                        expression.networkAttribute.dataType
                                    )
                                }
                    }.name
                    return networkAttributeNameAndOperator + codedValueDomainName
                }
                // if there's no coded value domain name
                    ?: return networkAttributeNameAndOperator + (expression.otherNetworkAttribute?.name
                        ?: expression.value)
            }
            // when the expression is an utility trace AND condition
            is UtilityTraceAndCondition -> {
                return expressionToString(expression.leftExpression) + " AND\n" + expressionToString(
                    expression.rightExpression
                )
            }
            // when the expression is an utility trace OR condition
            is UtilityTraceOrCondition -> {
                return expressionToString(expression.leftExpression) + " OR\n" + expressionToString(
                    expression.rightExpression
                )
            }
            else -> {
                return null
            }
        }
    }

    /**
     * Convert the given value into the correct Kotlin data type by using the attribute's data type.
     *
     * @param otherValue which will be converted
     * @param dataType to be converted to
     */
    private fun convertToDataType(
        otherValue: Any,
        dataType: UtilityNetworkAttributeDataType,
    ): Any {
        return try {
            when (dataType) {
                UtilityNetworkAttributeDataType.Boolean -> otherValue.toString().toBoolean()
                UtilityNetworkAttributeDataType.Double -> otherValue.toString().toDouble()
                UtilityNetworkAttributeDataType.Float -> otherValue.toString().toFloat()
                UtilityNetworkAttributeDataType.Integer -> otherValue.toString().toInt()
            }
        } catch (e: Exception) {
            ("Error converting data type: " + e.message).also {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                Log.e(TAG, it)
            }
        }
    }

    private fun showError(message: String) {
        Log.e(TAG, message)
        Snackbar.make(activityMainBinding.root, message, Snackbar.LENGTH_SHORT).show()
    }
}