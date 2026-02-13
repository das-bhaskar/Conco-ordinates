package com.example.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
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
import com.google.maps.android.PolyUtil
import java.io.IOException
import okhttp3.*
import org.json.JSONObject
class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mapManager: MapManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var mMap: GoogleMap

    // Tracks which campus is currently selected by the user
    private var currentVisibleCampus = CampusRepo.SGW

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

        // Initial view
        mapManager.focusOnCampus(currentVisibleCampus)

        startLocationUpdates()
        setupToggleLogic()
        setupRecenterButton()
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
            .setMinUpdateIntervalMillis(1000)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    val userLatLng = LatLng(location.latitude, location.longitude)

                    // Check building name for the campus the user is currently LOOKING at
                    val buildingName = mapManager.findBuildingAtLocation(userLatLng, currentVisibleCampus)

                    // IMPORTANT: Call a version of the focus function that DOES NOT move the camera
                    // You need to ensure MapManager has a function that only clears/redraws polygons
                    mapManager.updateHighlightsOnly(currentVisibleCampus, buildingName)
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
                        mapManager.focusOnCampus(currentVisibleCampus) // This one moves camera
                    }
                    R.id.btnLoyola -> {
                        currentVisibleCampus = CampusRepo.LOYOLA
                        mapManager.focusOnCampus(currentVisibleCampus) // This one moves camera
                    }
                }
            }
        }
    }

    private fun setupRecenterButton() {
        val fabRecenter = findViewById<FloatingActionButton>(R.id.fabRecenter)
        fabRecenter.setOnClickListener {
            mapManager.getUserLocation(fusedLocationClient) { userLatLng ->
                val cameraPosition = com.google.android.gms.maps.model.CameraPosition.Builder()
                    .target(userLatLng)
                    .zoom(18.5f)
                    .tilt(0f)
                    .build()

                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 1000, null)
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

    enum class TravelMode {
        PUB_TRANSIT,
        MOTORIZED,
        WALK,
    }

    // Send to callback function a path between two points
    fun reqPath(start: LatLng, end: LatLng, travelMode: TravelMode, callback: (List<LatLng>) -> Unit) {
        val mode = when (travelMode) {
            TravelMode.PUB_TRANSIT -> "transit"
            TravelMode.MOTORIZED -> "driving"
            TravelMode.WALK -> "walking"
        }

        val appInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
        val apiKey = appInfo.metaData.getString("com.google.android.geo.API_KEY")

        val url = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=${start.latitude},${start.longitude}" +
                "&destination=${end.latitude},${end.longitude}" +
                "&mode=$mode" +
                "&key=${apiKey}"

        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        // Requests route from google api
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                response.body.string().let { json ->
                    val points = decodeRoute(json)
                    runOnUiThread {
                        callback(points)
                    }
                }
            }
        })
    }

    // Returns parsed path given a valid json string and exception otherwise.
    private fun decodeRoute(json: String): List<LatLng> {
        val points = mutableListOf<LatLng>()
        try {
            val jsonObject = JSONObject(json)
            val routes = jsonObject.getJSONArray("routes")
            if (routes.length() > 0) {
                val lineBytes = routes.getJSONObject(0)
                    .getJSONObject("overview_polyline")
                    .getString("points")
                // Google api returns string of characters representing bytes
                // describing the points in a sequence. These need to be parsed
                // with a known algorithm before usable.
                points.addAll(PolyUtil.decode(lineBytes))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return points
    }
}