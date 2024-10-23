/*
 * Copyright 2023 Esri
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

package com.esri.arcgismaps.sample.setuplocationdrivengeotriggers

import android.content.DialogInterface
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.arcgismaps.data.ArcGISFeature
import com.esri.arcgismaps.sample.setuplocationdrivengeotriggers.databinding.FragmentFeatureViewBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch


/**
 * Class to display an ArcGISFeature [featureSection] information as a
 * bottom sheet dialog fragment. Provides an [onDismissListener] callback
 * when the fragment is dismissed.
 */
class FeatureViewFragment(
    private val featureSection: ArcGISFeature,
    private val onDismissListener: () -> Unit
) : BottomSheetDialogFragment() {

    // setup binding for the fragment
    private val featureViewFragmentBinding by lazy {
        FragmentFeatureViewBinding.inflate(layoutInflater)
    }

    // displays the feature title
    private val contentTitleText by lazy {
        featureViewFragmentBinding.contentTitleTextView
    }

    // displays the feature description
    private val contentDescriptionText by lazy {
        featureViewFragmentBinding.contentDescriptionTextView
    }

    // displays the primary image attachment of the feature
    private val contentImage by lazy {
        featureViewFragmentBinding.contentImageView
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        return featureViewFragmentBinding.root
    }

    override fun onStart() {
        super.onStart()
        // set up the UI
        // set the title text to the feature name
        contentTitleText.text = featureSection.attributes["name"] as String
        // set the description text to the feature description
        contentDescriptionText.text = featureSection.attributes["desc_raw"] as String
        // load the feature attachments
        loadAttachments()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        // call the dismiss listener
        onDismissListener()
    }

    /**
     * Loads the attachments of this ArcGISFeature and updates the UI
     */
    private fun loadAttachments() = lifecycleScope.launch {
        // fetch the list of attachments
        val attachments = featureSection.fetchAttachments().getOrElse {
            // return if it fails
            return@launch
        }
        // if there are attachments
        if (attachments.isNotEmpty()) {
            // get the first (and only) attachment for the feature, which is an image
            val imageAttachment = attachments.first()
            // and fetch its data
            val imageData = imageAttachment.fetchData().getOrElse {
                // return if it fails
                return@launch
            }
            // construct a bitmap from the fetched image data
            val bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
            // set the bitmap to the imageview
            contentImage.setImageBitmap(bitmap)
        }
    }
}
