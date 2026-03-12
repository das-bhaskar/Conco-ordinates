package com.example.myapplication.map

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState



public interface CameraController {
    suspend fun animateTo(target: LatLng, zoom: Float)
    // NEW: Modular profiles for different UX modes
    suspend fun resetToFlat(target: LatLng)
    suspend fun enterNavMode(target: LatLng, bearing: Float)
}

class TrueCameraController(private val state: CameraPositionState) : CameraController {

    override suspend fun animateTo(target: LatLng, zoom: Float) {
        state.animate(CameraUpdateFactory.newLatLngZoom(target, zoom), 1000)
    }

    override suspend fun resetToFlat(target: LatLng) {
        state.animate(
            update = CameraUpdateFactory.newCameraPosition(
                CameraPosition.Builder()
                    .target(target)
                    .zoom(state.position.zoom.coerceAtMost(16f))
                    .tilt(0f) // THE FIX: Reset tilt
                    .bearing(0f) // Reset to North
                    .build()
            ),
            durationMs = 1000
        )
    }

    // Inside TrueCameraController class
    override suspend fun enterNavMode(target: LatLng, bearing: Float) {
        state.animate(
            update = CameraUpdateFactory.newCameraPosition(
                CameraPosition.Builder()
                    .target(target)
                    .zoom(18.5f) // Slightly tighter for better walking detail
                    .tilt(50f)   // Aggressive tilt for that "Pro" feel
                    .bearing(bearing) // Face forward!
                    .build()
            ),
            durationMs = 800
        )
    }

}