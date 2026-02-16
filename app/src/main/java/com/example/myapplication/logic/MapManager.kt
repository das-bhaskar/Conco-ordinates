package com.example.myapplication.logic

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import android.media.Image
import androidx.compose.ui.graphics.asImageBitmap
import com.example.myapplication.data.Building
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.PolygonOptions
import com.example.myapplication.data.Campus
import com.example.myapplication.data.CampusRepo
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.maps.android.BuildConfig
import com.google.maps.android.PolyUtil
import okhttp3.OkHttpClient
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Locale


class MapManager(private val googleMap: GoogleMap) {

    // Task 1.5.1: Retrieve user's current location
    fun getUserLocation(fusedLocationClient: FusedLocationProviderClient, callback: (LatLng) -> Unit) {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val userLatLng = LatLng(location.latitude, location.longitude)
                    callback(userLatLng)
                }
            }
        } catch (e: SecurityException) {
            // Permission missing
        }
    }

    fun findBuildingAtLocation(userLocation: LatLng, campus: Campus): String? {
        var closestBuildingName: String? = null
        // 10 meters is ideal for downtown street density
        var shortestDistance = 10.0

        campus.buildings.forEach { building ->
            // 1. Check if user is strictly inside the polygon
            if (PolyUtil.containsLocation(userLocation, building.outline, false)) {
                return building.name
            }

            // 2. Check distance to the nearest edge of the building
            val distToPoly = distanceFromPoly(userLocation, building.outline)

            // 3. Update if this building is closer than the current best match
            if (distToPoly < shortestDistance) {
                shortestDistance = distToPoly
                closestBuildingName = building.name
            }
        }
        return closestBuildingName
    }

    //Check if user click is in one of the buildings and return it
    fun checkClickBuildings(latLng: LatLng): Building? {
        for(item in CampusRepo.getAllBuildings()){

            //Create poly from building outline and check if click is in it
            if(PolyUtil.containsLocation(latLng, item.outline, true)){
                return item;
            }
        }
        return null;
    }

    suspend fun getCenterLatLng(building: Building): String = withContext(Dispatchers.IO) {
        var response = ""
        val query = ("[out:csv(::lat,::lon)];way("+building.wayID+");out center;")

        val client = OkHttpClient()

        val requestBody =
            ("data=" + URLEncoder.encode(query, StandardCharsets.UTF_8.toString())).toRequestBody("application/x-www-form-urlencoded".toMediaType())

        val request = Request.Builder()
            .url("https://overpass-api.de/api/interpreter?")
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("Unexpected response: $response")
            }
            return@withContext response.body?.string() ?: ""
        }
    }

    suspend fun transformStringIntoLatLng(building: Building): LatLng{
        val address = getCenterLatLng(building).split("\n")[1]
        if(!address.isEmpty()) {
            return LatLng(address.split("\t")[0].toDouble(), address.split("\t")[1].toDouble())
        }
        return LatLng(0.0,0.0)
    }

    suspend fun getPanorama(latLng: LatLng): Bitmap? {
        if(Math.abs(latLng.latitude) > 0.01 && Math.abs(latLng.longitude) > 0.01){

            val apiKey = com.example.myapplication.BuildConfig.key

            val url = "https://maps.googleapis.com/maps/api/streetview" +
                    "?size=600x250" +
                    "&location=${latLng.latitude},${latLng.longitude}" +
                    "&key=$apiKey"


            val client = OkHttpClient()

            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw Exception("Unexpected response: $response")
                }

                val bytes = response.body?.bytes()
                return bytes?.let {
                    BitmapFactory.decodeByteArray(it, 0, it.size)
                }
            }
        }
        return null
    }

    fun getAddressFromLatLng(
        context: Context,
        latitude: Double,
        longitude: Double
    ): String? {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            var addresses: List<Address> =
                geocoder.getFromLocation(latitude, longitude, 1)?.toMutableList() ?: emptyList()

            if (!addresses.isNullOrEmpty()) {
                addresses[0].getAddressLine(0)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }




    // Helper to find the distance (in meters) from a point to the nearest polygon edge
    private fun distanceFromPoly(point: LatLng, poly: List<LatLng>): Double {
        var minDistance = Double.MAX_VALUE
        for (i in poly.indices) {
            val segmentStart = poly[i]
            val segmentEnd = poly[(i + 1) % poly.size]

            // Calculates distance from point to the line segment
            val distance = PolyUtil.distanceToLine(point, segmentStart, segmentEnd)

            if (distance < minDistance) {
                minDistance = distance
            }
        }
        return minDistance
    }

    // Use this for Manual Button clicks (SGW/Loyola/Recenter)
    fun focusOnCampus(campus: Campus, highlightedBuildingName: String? = null) {
        drawBuildings(campus, highlightedBuildingName)
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(campus.center, campus.defaultZoom))
    }

    // Task 1.5.4: Use this for Background GPS updates (No camera yanking)
    fun updateHighlightsOnly(campus: Campus, highlightedBuildingName: String? = null) {
        drawBuildings(campus, highlightedBuildingName)
    }

    private fun drawBuildings(campus: Campus, highlightedBuildingName: String?) {
        googleMap.clear()
        campus.buildings.forEach { building ->
            val isCurrentBuilding = building.name == highlightedBuildingName

            val polygon = PolygonOptions()
                .addAll(building.outline)
                .strokeWidth(if (isCurrentBuilding) 8f else 4f)
                .strokeColor(Color.parseColor("#912338"))
                .fillColor(if (isCurrentBuilding)
                    Color.argb(180, 145, 35, 56) else
                    Color.argb(80, 145, 35, 56))

            googleMap.addPolygon(polygon)
        }
    }
}