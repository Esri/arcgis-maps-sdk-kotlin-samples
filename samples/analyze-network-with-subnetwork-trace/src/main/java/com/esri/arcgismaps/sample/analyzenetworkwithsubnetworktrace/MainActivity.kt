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
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.CheckBox
import android.widget.RelativeLayout
import android.widget.TextView
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
import com.arcgismaps.data.ServiceGeodatabase
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
import com.esri.arcgismaps.sample.analyzenetworkwithsubnetworktrace.databinding.AnalyzeNetworkWithSubnetworkTraceActivityMainBinding
import com.esri.arcgismaps.sample.analyzenetworkwithsubnetworktrace.databinding.LoadingOptionsDialogBinding
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MainActivity : AppCompatActivity() {

    // set up data binding for the activity
    private val activityMainBinding: AnalyzeNetworkWithSubnetworkTraceActivityMainBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.analyze_network_with_subnetwork_trace_activity_main)
    }

    private val sourceDropdown: AutoCompleteTextView by lazy {
        activityMainBinding.sourceDropdown
    }

    private val operatorDropdown: AutoCompleteTextView by lazy {
        activityMainBinding.operatorDropdown
    }

    private val expressionTextView: TextView by lazy {
        activityMainBinding.expressionTextView
    }

    private val valuesDropdown: AutoCompleteTextView by lazy {
        activityMainBinding.valuesDropdown
    }

    private val valuesBackgroundView: RelativeLayout by lazy {
        activityMainBinding.valuesBackgroundView
    }

    private val valueBooleanButton: ToggleButton by lazy {
        activityMainBinding.valueBooleanButton
    }

    private val valuesEditText: TextInputEditText by lazy {
        activityMainBinding.valuesEditText
    }

    private val barriersCheckbox: CheckBox by lazy {
        activityMainBinding.barriersCheckBox
    }

    private val containersCheckbox: CheckBox by lazy {
        activityMainBinding.containersCheckbox
    }

    private val traceButton: MaterialButton by lazy {
        activityMainBinding.traceButton
    }

    private val utilityNetwork by lazy {
        UtilityNetwork(ServiceGeodatabase(getString(R.string.utility_network_url)))
    }

    private var initialExpression: UtilityTraceConditionalExpression? = null
    private var sourceTier: UtilityTier? = null
    private var utilityTraceConfiguration: UtilityTraceConfiguration? = null
    private var sourcesList: List<UtilityNetworkAttribute>? = null
    private var operatorsList: Array<UtilityAttributeComparisonOperator>? = null
    private var startingLocation: UtilityElement? = null
    private var codedValuesList: List<CodedValue>? = null
    private var sourcePosition: Int = 0
    private var operatorPosition: Int = 0
    private var valuePosition: Int = 0
    private var dialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ArcGISEnvironment.applicationContext = this
        ArcGISEnvironment.authenticationManager.arcGISAuthenticationChallengeHandler =
            getAuthenticationChallengeHandler()

        // create and display the loading dialog
        showLoadingDialog(true)

        // load the utility network
        lifecycleScope.launch {
            utilityNetwork.load().getOrElse {
                dialog?.dismiss()
                traceButton.isEnabled = false
                return@launch showError("Error loading utility network: ${it.message}")
            }

            // create a list of utility network attributes whose system is not defined
            sourcesList =
                utilityNetwork.definition?.networkAttributes?.filter { !it.isSystemDefined }

            sourceDropdown.apply {
                // add the list of sources to the drop down view
                setAdapter(sourcesList?.let { utilityNetworkAttributes ->
                    ArrayAdapter(
                        applicationContext,
                        com.esri.arcgismaps.sample.sampleslib.R.layout.custom_dropdown_item,
                        utilityNetworkAttributes.map { it.name })
                })

                // add an on item selected listener which calls on comparison source changed
                onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
                    sourcePosition = position
                    sourcesList?.get(position)?.let { onComparisonSourceChanged(it) }
                }
            }

            // create a list of utility attribute comparison operators
            operatorsList =
                UtilityAttributeComparisonOperator::class.sealedSubclasses.mapNotNull { it.objectInstance }
                    .toTypedArray()

            operatorDropdown.apply {
                // add the list of sources to the drop down view
                setAdapter(operatorsList?.let { utilityAttributeComparisonOperator ->
                    ArrayAdapter(applicationContext,
                        com.esri.arcgismaps.sample.sampleslib.R.layout.custom_dropdown_item,
                        utilityAttributeComparisonOperator.map { it::class.simpleName })
                })

                // add an on item selected listener which calls on comparison source changed
                onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
                    operatorPosition = position
                }
            }

            // create a default starting location
            val networkSource = utilityNetwork.definition?.getNetworkSource("Electric Distribution Device")

            val assetGroup = networkSource?.getAssetGroup("Circuit Breaker")

            val assetType = assetGroup?.getAssetType("Three Phase")

            val globalId = Guid("1CAF7740-0BF4-4113-8DB2-654E18800028")

            if (assetType == null) return@launch

            val terminal = assetType.terminalConfiguration?.terminals?.first { it.name == "Load" }

            // utility element to start the trace from
            startingLocation = utilityNetwork.createElementOrNull(assetType, globalId, terminal)

            // get a default trace configuration from a tier to update the UI
            val domainNetwork = utilityNetwork.definition?.getDomainNetwork(
                "ElectricDistribution"
            )

            // set source utility tier from the utility domain network
            sourceTier = domainNetwork?.getTier("Medium Voltage Radial")?.apply {
                utilityTraceConfiguration = getDefaultTraceConfiguration()
            }

            // set initial barrier condition
            val defaultConditionalExpression = sourceTier.let {
                utilityTraceConfiguration?.traversability?.barriers as UtilityTraceConditionalExpression
            }
            // set the text view
            expressionTextView.text = expressionToString(defaultConditionalExpression)
            // use the initial expression when resetting trace
            initialExpression = defaultConditionalExpression

            showLoadingDialog(false)
        }
    }

    /**
     * Returns a [ArcGISAuthenticationChallengeHandler] to access the utility network URL.
     */
    private fun getAuthenticationChallengeHandler(): ArcGISAuthenticationChallengeHandler {
        return ArcGISAuthenticationChallengeHandler { challenge ->
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
    }

    /**
     * When a comparison source [attribute] is chosen check if it's a coded value domain and, if it is,
     * present a dropdown of coded value domains. If not, show the correct UI view for the utility
     * network attribute data type.
     */
    private fun onComparisonSourceChanged(attribute: UtilityNetworkAttribute) {
        // if the domain is a coded value domain
        if (attribute.domain is CodedValueDomain) {
            (attribute.domain as CodedValueDomain).let { codedValueDomain ->
                // update the list of coded values
                codedValuesList = codedValueDomain.codedValues
                // show the values dropdown
                setVisible(valuesBackgroundView.id)
                // update the values dropdown adapter
                valuesDropdown.setAdapter(ArrayAdapter(applicationContext,
                    com.esri.arcgismaps.sample.sampleslib.R.layout.custom_dropdown_item,
                    // add the coded values from the coded value domain to the values dropdown
                    codedValueDomain.codedValues.map { it.name }))
                // add an on item selected listener which calls on comparison source changed
                valuesDropdown.onItemClickListener =
                    AdapterView.OnItemClickListener { _, _, position, _ ->
                        valuePosition = position
                    }
            }
        } // if the domain is not a coded value domain
        else {
            when (attribute.dataType) {
                UtilityNetworkAttributeDataType.Boolean -> {
                    // show true/false toggle button
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
    }

    /**
     * Add a new barrier condition to the trace options when [addConditionButton] is tapped.
     */
    fun addBarrierCondition(addConditionButton: View) {
        // if source tier doesn't contain a trace configuration, create one
        val traceConfiguration = utilityTraceConfiguration ?: UtilityTraceConfiguration().apply {
            // if the trace configuration doesn't contain traversability, create one
            traversability ?: UtilityTraversability()
        }

        // get the currently selected attribute
        sourcesList?.get(sourcePosition)?.let { sourceAttribute ->
            // if the other value is a coded value domain
            val otherValue = if (sourceAttribute.domain is CodedValueDomain) {
                codedValuesList?.get(valuePosition)?.code?.let {
                    convertToDataType(it, sourceAttribute.dataType)
                }
            } else {
                convertToDataType(valuesEditText.text.toString(), sourceAttribute.dataType)
            }

            if (otherValue.toString().contains("Error") || otherValue == null) {
                return showError(otherValue.toString())
            }

            // get the currently selected attribute operator>
            operatorsList?.get(operatorPosition)?.let { comparisonOperator ->
                // NOTE: You may also create a UtilityNetworkAttributeComparison
                // with another NetworkAttribute
                var expression: UtilityTraceConditionalExpression =
                    UtilityNetworkAttributeComparison(
                        sourceAttribute,
                        comparisonOperator,
                        otherValue
                    )
                (traceConfiguration.traversability?.barriers as? UtilityTraceConditionalExpression)?.let { otherExpression ->
                    // NOTE: You may also combine expressions with UtilityTraceAndCondition
                    expression = UtilityTraceOrCondition(otherExpression, expression)
                }
                traceConfiguration.traversability?.barriers = expression
                expressionTextView.text = expressionToString(expression)
            }
        }
    }

    /**
     * Show the UI of the given [id] and hide the others which share the same space.
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
     * Run the network trace with the parameters and display the result in an alert dialog
     * when the [traceButton] is clicked.
     */
    fun trace(traceButton: View) {
        if (utilityNetwork.loadStatus.value != LoadStatus.Loaded) {
            return showError("Utility network is not loaded")
        }

        // set the utility trace parameters
        val parameters = UtilityTraceParameters(
            UtilityTraceType.Subnetwork,
            listOf(startingLocation).requireNoNulls()
        ).apply {
            // set the utility trace configuration options to include
            traceConfiguration = utilityTraceConfiguration?.apply {
                includeBarriers = barriersCheckbox.isChecked
                includeContainers = containersCheckbox.isChecked
            }
        }

        // launch trace in a coroutine scope
        lifecycleScope.launch {
            showLoadingDialog(true)
            val utilityTraceResults = utilityNetwork.trace(parameters).getOrElse {
                return@launch showError(it.message + getString(R.string.example_condition))
            }
            // get the UtilityElementTraceResult
            val elementTraceResult = utilityTraceResults.first() as UtilityElementTraceResult

            showLoadingDialog(false)
            MaterialAlertDialogBuilder(this@MainActivity).apply {
                // set the result dialog title
                setTitle("Trace result")
                // show the element result count
                setMessage(elementTraceResult.elements.count().toString() + " elements found.")
            }.show()
        }
    }

    /**
     * Convert the given [expression] into a string.
     */
    private fun expressionToString(expression: UtilityTraceConditionalExpression): String? {
        when (expression) {
            // when the expression is a category comparison expression
            is UtilityCategoryComparison -> {
                return expression.category.name + " " + expression.comparisonOperator
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
            // when the expression is an attribute comparison expression
            is UtilityNetworkAttributeComparison -> {
                // the name and comparison operator of the expression
                val networkAttributeNameAndOperator = expression.networkAttribute.name + " " +
                        expression.comparisonOperator::class.simpleName + " "

                // check whether the network attribute has a coded value domain
                val codedValueDomain = expression.networkAttribute.domain as? CodedValueDomain
                return if (codedValueDomain != null) {
                    networkAttributeNameAndOperator +
                            getCodedValueFromExpression(codedValueDomain, expression)?.name
                } else {
                    // if there's no coded value domain
                    networkAttributeNameAndOperator +
                            (expression.otherNetworkAttribute?.name ?: expression.value)
                }
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
            if (dataType::class.isInstance(UtilityNetworkAttributeDataType.Boolean)) {
                otherValue.toString().toBoolean()
            } else if (dataType::class.isInstance(UtilityNetworkAttributeDataType.Double)) {
                otherValue.toString().toDouble()
            } else if (dataType::class.isInstance(UtilityNetworkAttributeDataType.Float)) {
                otherValue.toString().toFloat()
            } else if (dataType::class.isInstance(UtilityNetworkAttributeDataType.Integer)) {
                otherValue.toString().toInt()
            } else {
            }
        } catch (e: Exception) {
            return ("Error converting value to a datatype")
        }
    }

    /**
     * Returns a [CodedValue] found in the [expression] using the
     * list of coded values in the [codedValueDomain].
     */
    private fun getCodedValueFromExpression(
        codedValueDomain: CodedValueDomain,
        expression: UtilityNetworkAttributeComparison,
    ): CodedValue? {
        // if there's a coded value domain name
        return codedValueDomain.codedValues.first { codedValue ->
            val code = codedValue.code
            val value = expression.value
            if (code != null && value != null) {
                return@first (convertToDataType(code,
                    expression.networkAttribute.dataType) == convertToDataType(value,
                    expression.networkAttribute.dataType))
            } else
                return null
        }
    }

    /**
     * Reset the current barrier condition to the initial expression
     * "Operational Device Status EQUAL Open" and resets the UI.
     */
    fun reset(view: View) {
        initialExpression?.let {
            utilityTraceConfiguration = sourceTier?.getDefaultTraceConfiguration()?.apply {
                traversability?.barriers = it
            }
            expressionTextView.text = expressionToString(it)
        }
    }

    private fun showLoadingDialog(isVisible: Boolean) {
        if (isVisible) {
            dialog = MaterialAlertDialogBuilder(this).apply {
                setCancelable(false)
                setView(LoadingOptionsDialogBinding.inflate(layoutInflater).root)
            }.show()
        } else {
            dialog?.dismiss()
        }
    }

    private fun showError(message: String) {
        Log.e(localClassName, message)
        Snackbar.make(activityMainBinding.root, message, Snackbar.LENGTH_SHORT).show()
    }
}
