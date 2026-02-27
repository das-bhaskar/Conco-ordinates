package com.example.myapplication.logic

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.location.FusedLocationProviderClient
import com.example.myapplication.telemetry.CrashReporter


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
            CrashReporter.setKey("location_provider", "TrueLocationProvider")
            CrashReporter.recordNonFatal(e, "location_permission_missing_in_provider")
            callback(null)
        }
    }
}
