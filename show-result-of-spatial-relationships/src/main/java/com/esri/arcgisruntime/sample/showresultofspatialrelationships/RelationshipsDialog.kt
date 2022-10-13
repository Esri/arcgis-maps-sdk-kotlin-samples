package com.esri.arcgisruntime.sample.showresultofspatialrelationships

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.BaseExpandableListAdapter
import android.widget.ExpandableListView
import android.widget.ListView
import android.widget.TextView
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class RelationshipsDialog {

    fun createDialog(
        layoutInflater: LayoutInflater,
        context: MainActivity,
        relationships: HashMap<String, List<String>>
    ) {
        val dialogView: View = layoutInflater.inflate(R.layout.dialog_layout, null)
        val dialogBuilder = MaterialAlertDialogBuilder(context).setView(dialogView)

        val polylineListView = dialogView.findViewById<ListView>(R.id.polylineListView)
        val polygonListView = dialogView.findViewById<ListView>(R.id.polygonListView)
        val pointListView = dialogView.findViewById<ListView>(R.id.pointListView)

        val polylineRelationships = relationships["Polyline"]
        val pointRelationships = relationships["Point"]
        val polygonRelationships = relationships["Polygon"]

        if (polylineRelationships.isNullOrEmpty() || pointRelationships.isNullOrEmpty() || polygonRelationships.isNullOrEmpty())
            return

        polylineListView.adapter = ArrayAdapter(
            dialogView.context, android.R.layout.simple_list_item_1,
            polylineRelationships
        )
        polygonListView.adapter = ArrayAdapter(
            dialogView.context, android.R.layout.simple_list_item_1,
            polygonRelationships
        )
        pointListView.adapter = ArrayAdapter(
            dialogView.context, android.R.layout.simple_list_item_1,
            pointRelationships
        )

        setDynamicHeight(polylineListView)
        setDynamicHeight(polygonListView)
        setDynamicHeight(pointListView)

        dialogBuilder.setNeutralButton("Dismiss") { dialog, _ ->
            dialog.dismiss()
        }
        dialogBuilder.create().show()
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
