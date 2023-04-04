package com.esri.arcgismaps.sample.editfeatureattachments

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.arcgismaps.data.Attachment
import com.esri.arcgismaps.sample.editfeatureattachments.databinding.AttachmentEditSheetBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch

class AttachmentsBottomSheet(
    context: MainActivity,
    bottomSheetBinding: AttachmentEditSheetBinding,
    attachments: List<Attachment>,
    damageType: String,
) : BottomSheetDialog(context) {
    init {
        // clear and set bottom sheet content view to layout,
        // to be able to set the content view on each bottom sheet draw
        if (bottomSheetBinding.root.parent != null) {
            (bottomSheetBinding.root.parent as ViewGroup).removeAllViews()
        }

        // set the state to be an expanded sheet
        behavior.state = BottomSheetBehavior.STATE_EXPANDED

        bottomSheetBinding.apply {
            // set the selected feature's information
            damageStatus.text = String.format("Damage type: %s", damageType)
            numberOfAttachments.text = String.format("Number of attachments: %d", attachments.size)
            // get the adapter to display the list of attachments
            listView.adapter = AttachmentsAdapter(context, attachments)

            // listener to add a new attachment
            addAttachmentButton.setOnClickListener {
                context.selectAttachment()
            }

            // listener to download and open an attachment
            listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
                lifecycleScope.launch {
                    context.fetchAttachment(
                        attachments[position]
                    )
                }
            }

            // listener to delete an attachment
            listView.onItemLongClickListener =
                AdapterView.OnItemLongClickListener { _, _, position, _ ->
                    // create a dialog to display the delete query
                    val builder = AlertDialog.Builder(context)
                    builder.setMessage(context.getString(R.string.delete_query))
                    builder.setCancelable(true)
                    builder.setPositiveButton(context.getString(R.string.yes)) { dialog, _ ->
                        // user confirmed, delete selected attachment
                        context.deleteAttachment(attachments[position])
                        dialog.dismiss()
                    }
                    builder.setNegativeButton(context.getString(R.string.no)) { dialog, _ ->
                        dialog.cancel()
                    }
                    val alert = builder.create()
                    alert.show()
                    true
                }

            // set apply button to validate and apply contingency feature on map
            applyTv.setOnClickListener {
                this@AttachmentsBottomSheet.dismiss()
            }
        }
    }

    class AttachmentsAdapter(
        private val context: Activity,
        private val attachmentName: List<Attachment>,
    ) : ArrayAdapter<Attachment>(context, R.layout.attachment_entry, attachmentName) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: context.layoutInflater.inflate(
                R.layout.attachment_entry,
                parent,
                false
            )
            val attachmentTextView = view.findViewById<TextView>(R.id.AttachmentName)
            attachmentTextView.text = attachmentName[position].name
            return view
        }
    }
}
