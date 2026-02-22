package com.example.myapplication

import com.example.myapplication.map.CameraController
import com.google.android.gms.maps.model.LatLng

class CameraControllerTest : CameraController {
    var lastTarget: LatLng? = null
    var lastZoom: Float? = null

    override suspend fun animateTo( target: LatLng, zoom: Float) {
        lastTarget = target
        lastZoom = zoom
    }
}