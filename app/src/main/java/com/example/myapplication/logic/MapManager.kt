package com.example.myapplication.logic

import android.graphics.Color
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.PolygonOptions
import com.example.myapplication.data.Campus

class MapManager(private val googleMap: GoogleMap) {


    fun focusOnCampus(campus: Campus) {
        googleMap.clear()

        campus.buildings.forEach { building ->
            val polygon = PolygonOptions()
                .addAll(building.outline)
                .strokeColor(Color.parseColor("#912338")) // Concordia Maroon
                .strokeWidth(4f)
                .fillColor(Color.argb(80, 145, 35, 56)) // Semi-transparent Maroon

            googleMap.addPolygon(polygon)
        }

        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(campus.center, campus.defaultZoom))
    }
}