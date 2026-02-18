package com.example.myapplication

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class BottomSheetPopUp : BottomSheetDialogFragment() {
    private var address: String? = null
    private var smallAddress: String? = null
    private lateinit var addressViewText: TextView
    private var buildingPicture: ImageView? = null
    private var buildingBitmap: Bitmap? = null

    private lateinit var smallAddressViewText: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.bottom_sheet_building_info, container, false)

        addressViewText = view.findViewById(R.id.txtBuildingAddress)
        buildingPicture = view.findViewById(R.id.buildingView)
        smallAddressViewText = view.findViewById(R.id.txtAddressSmall)

        buildingBitmap?.let {buildingPicture?.setImageBitmap(it)}


        address?.let { addressViewText.text = it }
        smallAddress?.let { smallAddressViewText.text = it }

        return view
    }

    fun setAddress(addressInput: String) {
        address = addressInput
        if (::addressViewText.isInitialized) {
            addressViewText.text = addressInput
        }
    }

    fun setSmallAddress(addressInput: String) {
        smallAddress = addressInput
        if (::smallAddressViewText.isInitialized) {
            smallAddressViewText.text = addressInput
        }
    }

    fun setBuildingPicture(image: Bitmap) {
        buildingBitmap = image
        buildingPicture?.setImageBitmap(image)
    }
}

