package com.example.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.myapplication.data.Campus
import com.example.myapplication.data.CampusRepo
import com.example.myapplication.logic.MapManager
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mapManager: MapManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var mMap: GoogleMap

    // Tracks which campus is currently selected by the user
    private var currentVisibleCampus: Campus? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mapManager = MapManager(googleMap)

        mMap.uiSettings.isTiltGesturesEnabled = false
        mMap.uiSettings.isMyLocationButtonEnabled = false

        enableMyLocation()

        // --- FIX START ---
        mapManager.getUserLocation(fusedLocationClient) { userLatLng ->
            if (userLatLng != null) {
                // 1. Move camera to where the user actually is
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 16f))

                // 2. Determine if they are on a campus (using your teammate's new logic)
                val detectedCampus = CampusRepo.getCampus(userLatLng)
                currentVisibleCampus = detectedCampus

                // 3. Update the UI buttons to match
                updateToggleUI(detectedCampus)
            } else {
                // Fallback: If GPS is off, default to SGW so the map isn't blank
                currentVisibleCampus = CampusRepo.SGW
                mapManager.focusOnCampus(CampusRepo.SGW)
                updateToggleUI(CampusRepo.SGW)
            }
        }
        // --- FIX END ---

        startLocationUpdates()
        setupToggleLogic()
        setupRecenterButton()
    }

    private fun updateToggleUI(campus: Campus?) {
        val toggleGroup = findViewById<MaterialButtonToggleGroup>(R.id.toggleGroup)
        when (campus) {
            CampusRepo.SGW -> toggleGroup.check(R.id.btnSgw)
            CampusRepo.LOYOLA -> toggleGroup.check(R.id.btnLoyola)
            null -> toggleGroup.clearChecked()
        }
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
            .setMinUpdateIntervalMillis(1000)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    val userLatLng = LatLng(location.latitude, location.longitude)

                    // 1. It checks if currentVisibleCampus is null (fixing the type mismatch)
                    // 2. It keeps 'buildingName' inside the scope (fixing the unresolved reference)
                    currentVisibleCampus?.let { campus ->
                        val buildingName = mapManager.findBuildingAtLocation(userLatLng, campus)
                        mapManager.updateHighlightsOnly(campus, buildingName)
                    }
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, mainLooper)
        }
    }

    private fun setupToggleLogic() {
        val toggleGroup = findViewById<MaterialButtonToggleGroup>(R.id.toggleGroup)
        toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btnSgw -> {
                        currentVisibleCampus = CampusRepo.SGW
                        mapManager.focusOnCampus(currentVisibleCampus!!)
                    }
                    R.id.btnLoyola -> {
                        currentVisibleCampus = CampusRepo.LOYOLA
                        mapManager.focusOnCampus(currentVisibleCampus!!)
                    }
                }
            } else {
                // If no buttons are pressed in the group -> we set the state to null so the app knows we are "off-campus"
                if (toggleGroup.checkedButtonId == -1) {
                    currentVisibleCampus = null
                }
            }
        }
    }

    private fun setupRecenterButton() {
        val fabRecenter = findViewById<FloatingActionButton>(R.id.fabRecenter)
        fabRecenter.setOnClickListener {
            mapManager.getUserLocation(fusedLocationClient) { userLatLng ->
                userLatLng?.let { latLng ->
                    // 1. Move the camera to current location
                    val cameraPosition = com.google.android.gms.maps.model.CameraPosition.Builder()
                        .target(latLng)
                        .zoom(18.5f)
                        .tilt(0f)
                        .build()
                    mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 1000, null)

                    // 2. RE-EVALUATE the campus state
                    val detectedCampus = CampusRepo.getCampus(latLng)

                    // 3. Update the state and UI
                    currentVisibleCampus = detectedCampus
                    updateToggleUI(detectedCampus)
                }
            }
        }
    }

    private fun enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.isMyLocationEnabled = true
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }
    }

    override fun onPause() {
        super.onPause()
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }
}