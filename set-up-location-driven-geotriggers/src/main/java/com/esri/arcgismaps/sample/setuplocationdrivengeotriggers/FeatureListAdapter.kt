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
    private val featuresList: MutableList<ArcGISFeature>,
    private var onItemClickListener: (ArcGISFeature) -> Unit
) : RecyclerView.Adapter<FeatureViewHolder>() {

    override fun onBindViewHolder(holder: FeatureViewHolder, position: Int) {
        // bind the feature information to each item view
        val feature = featuresList[position]
        // set the title
        holder.titleView.text = feature.attributes["name"].toString()
        // set the onClickListener to pass the item's feature
        holder.itemView.setOnClickListener {
            onItemClickListener.invoke(feature)
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