package com.example.myapplication

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.data.CampusRepo
import com.example.myapplication.logic.MapManager
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.material.button.MaterialButtonToggleGroup

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mapManager: MapManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // This links directly to the XML layout you provided
        setContentView(R.layout.activity_maps)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        // Initialize logic and default to Satellite View
        mapManager = MapManager(googleMap)

        // Focus on SGW immediately
        mapManager.focusOnCampus(CampusRepo.SGW)

        setupToggleLogic()
    }

    private fun setupToggleLogic() {
        val toggleGroup = findViewById<MaterialButtonToggleGroup>(R.id.toggleGroup)
        toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btnSgw -> mapManager.focusOnCampus(CampusRepo.SGW)
                    R.id.btnLoyola -> mapManager.focusOnCampus(CampusRepo.LOYOLA)
                }
            }
        }
    }
}