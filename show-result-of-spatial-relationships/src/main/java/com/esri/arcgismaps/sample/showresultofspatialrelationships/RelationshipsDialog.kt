package com.esri.arcgismaps.sample.showresultofspatialrelationships

import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import com.arcgismaps.data.SpatialRelationship
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Class to create and display a dialog of the selected geometry's spatial relationships
 */
class RelationshipsDialog(
    private val layoutInflater: LayoutInflater,
    private val context: MainActivity,
    private val relationships: Map<String, List<SpatialRelationship>>,
    private val selectedGraphicName: String
) {

    fun createAndDisplayDialog() {
        // get the dialog root view
        val dialogView: View = layoutInflater.inflate(R.layout.dialog_layout, null)
        // set up the dialog builder and the title
        val dialogBuilder = MaterialAlertDialogBuilder(context).setView(dialogView)
        dialogBuilder.setTitle("$selectedGraphicName geometry's relationship")
        // set up the dialog UI views
        val polylineListView = dialogView.findViewById<ListView>(R.id.polylineListView)
        val polygonListView = dialogView.findViewById<ListView>(R.id.polygonListView)
        val pointListView = dialogView.findViewById<ListView>(R.id.pointListView)
        val polylineTitle = dialogView.findViewById<TextView>(R.id.polylineTitle)
        val polygonTitle = dialogView.findViewById<TextView>(R.id.polygonTitle)
        val pointTitle = dialogView.findViewById<TextView>(R.id.pointTitle)
        // get the spatial relations of each geometry as List<String>
        val pointRelationships = relationships["Point"].convertToStringList()
        val polygonRelationships = relationships["Polygon"].convertToStringList()
        val polylineRelationships = relationships["Polyline"].convertToStringList()
        // display list if polyline has relationships
        if (polylineRelationships.isNotEmpty()) {
            polylineListView.adapter = ArrayAdapter(
                dialogView.context, R.layout.custom_dropdown_item,
                polylineRelationships
            )
        } else polylineTitle.visibility = View.GONE
        // display list if polygon has relationships
        if (polygonRelationships.isNotEmpty()) {
            polygonListView.adapter = ArrayAdapter(
                dialogView.context, R.layout.custom_dropdown_item,
                polygonRelationships
            )
        } else polygonTitle.visibility = View.GONE
        // display list if point has relationships
        if (pointRelationships.isNotEmpty()) {
            pointListView.adapter = ArrayAdapter(
                dialogView.context, R.layout.custom_dropdown_item,
                pointRelationships
            )
        } else pointTitle.visibility = View.GONE
        // set the heights of each list view to make the entire view scrollable
        setDynamicHeight(polylineListView)
        setDynamicHeight(polygonListView)
        setDynamicHeight(pointListView)
        // create and display the alert dialog
        dialogBuilder.create().show()
    }

    /**
     * Dynamically sets the height of the list view to make the entire
     * dialog scrollable, instead of multiple scrolling views.
     */
    private fun setDynamicHeight(listView: ListView) {
        val adapter = listView.adapter ?: return
        var height = 0
        val desiredWidth =
            View.MeasureSpec.makeMeasureSpec(listView.width, View.MeasureSpec.UNSPECIFIED)
        for (index in 0 until adapter.count) {
            val listItem = adapter.getView(index, null, listView)
            listItem.measure(desiredWidth, View.MeasureSpec.UNSPECIFIED)
            height += listItem.measuredHeight
        }
        val layoutParams = listView.layoutParams
        layoutParams.height = height + (listView.dividerHeight * (adapter.count - 1))
        listView.layoutParams = layoutParams
        listView.requestLayout()
    }
}

/**
 * Extension function to convert the list of spatial relationships to a list of strings
 */
private fun List<SpatialRelationship>?.convertToStringList(): List<String> {
    val list = mutableListOf<String>()
    this?.forEach {
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
