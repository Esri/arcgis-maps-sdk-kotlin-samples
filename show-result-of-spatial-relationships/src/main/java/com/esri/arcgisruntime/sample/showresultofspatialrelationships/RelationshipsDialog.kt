package com.esri.arcgisruntime.sample.showresultofspatialrelationships

import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import arcgisruntime.data.SpatialRelationship
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class RelationshipsDialog {

    fun createDialog(
        layoutInflater: LayoutInflater,
        context: MainActivity,
        relationships: HashMap<String, List<SpatialRelationship>>
    ) {
        val dialogView: View = layoutInflater.inflate(R.layout.dialog_layout, null)
        val dialogBuilder = MaterialAlertDialogBuilder(context).setView(dialogView)
        // create an expandable list view and an adapter
        val polylineListView = dialogView.findViewById<ListView>(R.id.polylineListView)
        val polygonListView = dialogView.findViewById<ListView>(R.id.polygonListView)
        val pointListView = dialogView.findViewById<ListView>(R.id.pointListView)


        var adapter = ArrayAdapter<SpatialRelationship>(context,android.R.layout.simple_list_item_1)
        relationships["Polyline"]?.let { adapter.addAll(it) }
        polylineListView.adapter = adapter

        relationships["Polygon"]?.let { adapter.addAll(it) }
        polygonListView.adapter = adapter

        relationships["Point"]?.let { adapter.addAll(it) }
        pointListView.adapter = adapter

        dialogBuilder.setNeutralButton("Dismiss") { dialog, _ ->
            dialog.dismiss()
        }
        dialogBuilder.create().show()
    }

}
