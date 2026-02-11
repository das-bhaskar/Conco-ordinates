package com.example.myapplication

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class BottomSheetPopUp : BottomSheetDialogFragment() {
    private var address: String? = null
    private lateinit var addressViewText: TextView
    private var buildingPicture: ImageView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.bottom_sheet_building_info, container, false)

        addressViewText = view.findViewById(R.id.buildingAddress)
        buildingPicture = view.findViewById(R.id.buildingView)

        address?.let{addressViewText.text = it}

        return view
    }

    fun setAddress(addressInput: String) {
            address = addressInput
        if(::addressViewText.isInitialized){
            addressViewText.text = addressInput
        }
    }

    private fun setImageInternal(image: Any) {
        when (image) {
            is Bitmap -> buildingPicture?.setImageBitmap(image)
            is Uri -> buildingPicture?.setImageURI(image)
            is Int -> buildingPicture?.setImageResource(image) // drawable res         }
        }
    }
}

