package com.example.myapplication.logic

import com.google.android.gms.maps.model.LatLng

class MockLocationProvider : LocationProvider {
    var mockedLocation: LatLng? = null

    override fun getUserLocation(callback: (LatLng?) -> Unit) {
        callback(mockedLocation)
    }
}