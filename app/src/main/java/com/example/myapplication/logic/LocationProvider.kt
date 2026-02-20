package com.example.myapplication.logic

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.location.FusedLocationProviderClient


interface LocationProvider {
    fun getUserLocation(callback: (LatLng?) -> Unit)
}


class TrueLocationProvider(private val client: FusedLocationProviderClient) : LocationProvider {
    override fun getUserLocation(callback: (LatLng?) -> Unit) {
        try {
            client.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    callback(LatLng(location.latitude, location.longitude))
                } else {
                    callback(null)
                }
            }
        } catch (e: SecurityException) {
            callback(null)
        }
    }
}