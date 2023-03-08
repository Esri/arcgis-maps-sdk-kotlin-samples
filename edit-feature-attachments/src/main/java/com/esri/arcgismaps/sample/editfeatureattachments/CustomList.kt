package com.esri.arcgismaps.sample.editfeatureattachments

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView


class CustomList(private val context: Activity, private val attachmentName: List<String>) :
    ArrayAdapter<String>(context, R.layout.attachment_entry, attachmentName) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView
        val holder: ViewHolder
        if (convertView == null) {
            val inflater = context.layoutInflater
            convertView = inflater.inflate(R.layout.attachment_entry, null, true)
            holder = ViewHolder()
            holder.textTitle = convertView.findViewById(R.id.AttachmentName)
            convertView.tag = holder
        } else {
            holder = convertView.tag as ViewHolder
        }
        holder.textTitle!!.text = attachmentName[position]
        return convertView!!
    }

    private class ViewHolder {
        var textTitle: TextView? = null
    }
}

