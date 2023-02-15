package com.esri.arcgismaps.sample.setuplocationdrivengeotriggers

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.arcgismaps.data.ArcGISFeature

class FeatureViewHolder(view: View) : ViewHolder(view) {
    val title: TextView

    init {
        title = view.findViewById(R.id.featureNameTextView)
    }
}

class FeatureListAdapter(private val data: MutableList<ArcGISFeature>) : RecyclerView.Adapter<FeatureViewHolder>() {

    private var onItemClickListener : ((ArcGISFeature) -> Unit)? = null

    override fun onBindViewHolder(holder: FeatureViewHolder, position: Int) {
        val feature = data[position]
        holder.title.text = feature.attributes["name"].toString()
        holder.itemView.setOnClickListener {
            onItemClickListener?.invoke(feature)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeatureViewHolder {
        val view =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.feature_list_item, parent, false)
        return FeatureViewHolder(view)
    }

    override fun getItemCount(): Int {
        return data.size
    }

    fun setOnItemClickListener(listener: (ArcGISFeature) -> Unit) {
        onItemClickListener = listener
    }
}