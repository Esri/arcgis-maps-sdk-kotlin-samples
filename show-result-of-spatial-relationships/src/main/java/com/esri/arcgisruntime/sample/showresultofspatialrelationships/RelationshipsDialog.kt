package com.esri.arcgisruntime.sample.showresultofspatialrelationships

import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
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
        dialogBuilder.setTitle("Selected geometry relationships:")

        val polylineListView = dialogView.findViewById<ListView>(R.id.polylineListView)
        val polygonListView = dialogView.findViewById<ListView>(R.id.polygonListView)
        val pointListView = dialogView.findViewById<ListView>(R.id.pointListView)
        val polylineTitle = dialogView.findViewById<TextView>(R.id.polylineTitle)
        val polygonTitle = dialogView.findViewById<TextView>(R.id.polygonTitle)
        val pointTitle = dialogView.findViewById<TextView>(R.id.pointTitle)

        val pointRelationships = getStringRelationships(relationships["Point"])
        val polygonRelationships = getStringRelationships(relationships["Polygon"])
        val polylineRelationships = getStringRelationships(relationships["Polyline"])

        if (polylineRelationships.isNotEmpty()) {
            polylineListView.adapter = ArrayAdapter(
                dialogView.context, android.R.layout.simple_list_item_1,
                polylineRelationships
            )
        } else {
            polylineTitle.visibility = View.GONE
        }

        if (polygonRelationships.isNotEmpty()) {
            polygonListView.adapter = ArrayAdapter(
                dialogView.context, android.R.layout.simple_list_item_1,
                polygonRelationships
            )
        } else {
            polygonTitle.visibility = View.GONE
        }

        if (pointRelationships.isNotEmpty()) {
            pointListView.adapter = ArrayAdapter(
                dialogView.context, android.R.layout.simple_list_item_1,
                pointRelationships
            )
        } else {
            pointTitle.visibility = View.GONE
        }

        setDynamicHeight(polylineListView)
        setDynamicHeight(polygonListView)
        setDynamicHeight(pointListView)

        dialogBuilder.setNeutralButton("Dismiss") { dialog, _ ->
            dialog.dismiss()
        }
        dialogBuilder.create().show()
    }

    private fun getStringRelationships(relationships: List<SpatialRelationship>?): MutableList<String> {
        val list = mutableListOf<String>()
        relationships?.forEach {
            when (it) {
                SpatialRelationship.Crosses -> list.add("Crosses")
                SpatialRelationship.Contains -> list.add("Contains")
                SpatialRelationship.Disjoint -> list.add("Disjoint")
                SpatialRelationship.Intersects -> list.add("Intersects")
                SpatialRelationship.Overlaps -> list.add("Overlaps")
                SpatialRelationship.Touches -> list.add("Touches")
                SpatialRelationship.Within -> list.add("Within")
                else -> {}
            }
        }
        return list
    }

    private fun setDynamicHeight(listView: ListView) {
        val adapter = listView.adapter ?: return
        var height = 0
        val desiredWidth =
            View.MeasureSpec.makeMeasureSpec(listView.width, View.MeasureSpec.UNSPECIFIED)
        var index = 0
        while (index < adapter.count) {
            val listItem = adapter.getView(index, null, listView)
            listItem.measure(desiredWidth, View.MeasureSpec.UNSPECIFIED)
            height += listItem.measuredHeight
            index++
        }
        val layoutParams = listView.layoutParams
        layoutParams.height = height + (listView.dividerHeight * (adapter.count - 1))
        listView.layoutParams = layoutParams
        listView.requestLayout()
    }

}
