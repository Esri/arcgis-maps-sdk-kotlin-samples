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

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.arcgismaps.data.ArcGISFeature

/**
 * Provides a reference view holder to a custom view type
 * for the FeatureListAdapter
 */
class FeatureViewHolder(view: View) : ViewHolder(view) {
    // shows the title information
    val titleView: TextView by lazy {
        view.findViewById(R.id.featureNameTextView)
    }
}

/**
 * Custom RecyclerView Adapter to display the list of points of interests [featuresList]
 * [onItemClickListener] event is called when an itemView clicks
 */
class FeatureListAdapter(
    private val featuresList: List<ArcGISFeature>,
    private var onItemClickListener: (ArcGISFeature) -> Unit
) : RecyclerView.Adapter<FeatureViewHolder>() {

    override fun onBindViewHolder(holder: FeatureViewHolder, position: Int) {
        // bind the feature information to each item view
        val feature = featuresList[position]
        // set the title
        holder.titleView.text = feature.attributes["name"].toString()
        // set the onClickListener to pass the item's feature
        holder.titleView.setOnClickListener {
            onItemClickListener(feature)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeatureViewHolder {
        // inflate the item's view with the layout resource
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.feature_list_item, parent, false)
        return FeatureViewHolder(view)
    }

    override fun getItemCount(): Int {
        return featuresList.size
    }
}
