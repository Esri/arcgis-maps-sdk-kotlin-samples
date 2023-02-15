package com.esri.arcgismaps.sample.setuplocationdrivengeotriggers

//import com.esri.arcgismaps.sample.setuplocationdrivengeotriggers.databinding
import android.content.DialogInterface
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.arcgismaps.data.ArcGISFeature
import com.esri.arcgismaps.sample.setuplocationdrivengeotriggers.databinding.FragmentFeatureViewBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch
import java.io.File

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [FeatureViewFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class FeatureViewFragment(val featureSection: ArcGISFeature) : BottomSheetDialogFragment() {

    private val TAG = MainActivity::class.java.simpleName

    private val featureViewFragmentBinding by lazy {
        FragmentFeatureViewBinding.inflate(layoutInflater)
    }

    private val contentTitleText by lazy {
        featureViewFragmentBinding.contentTitleTextView
    }

    private val contentDescriptionText by lazy {
        featureViewFragmentBinding.contentDescriptionTextView
    }

    private val contentImage by lazy {
        featureViewFragmentBinding.contentImageView
    }

    private val thumbnailFileDir by lazy {
        context?.getExternalFilesDir(null)?.path + File.separator
    }

    private var onDismissListener: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        return featureViewFragmentBinding.root
    }

    override fun onStart() {
        super.onStart()
        // set ui elements
        contentTitleText.text = featureSection.attributes["name"] as String
        contentDescriptionText.text = featureSection.attributes["desc_raw"] as String
        loadAttachments()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDismissListener?.invoke()
    }

    fun setOnDismissListener(listener: () -> Unit) {
        onDismissListener = listener
    }

    private fun loadAttachments() = lifecycleScope.launch {
        val attachments = featureSection.fetchAttachments().getOrElse {
            return@launch
        }

        if (attachments.isNotEmpty()) {
            val imageData = attachments.first().fetchData().getOrElse {
                return@launch
            }

            val bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
            contentImage.setImageBitmap(bitmap)
        }
    }
}