package com.example.myapplication.map

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState

public interface CameraController {
    suspend fun animateTo(target:LatLng, zoom: Float)

}

class TrueCameraController (private val state: CameraPositionState): CameraController {

    override suspend fun animateTo(target: LatLng, zoom: Float) {
        state.animate(
            update = CameraUpdateFactory.newLatLngZoom(target, zoom),
            durationMs = 1000
        )
    }
}