package com.example.myapplication.logic

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import android.media.Image
import android.util.Log
import androidx.compose.ui.graphics.asImageBitmap
import com.example.myapplication.data.Building
import androidx.core.graphics.toColorInt
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

    fun getUserLocation(fusedLocationClient: FusedLocationProviderClient, callback: (LatLng?) -> Unit) {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                // If location is null, we pass null to the callback so the Activity can handle the fallback
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

    //Check if user click on one of the buildings
    fun checkClickBuildings(latLng: LatLng): Building? {
        for(item in CampusRepo.getAllBuildings()){
            if(PolyUtil.containsLocation(latLng, item.outline, true)){
                return item;
            }
        }
        return null;
    }

    suspend fun getCenterLatLng(building: Building): LatLng {
        if(building.wayID <=0){
            return LatLng(0.0, 0.0)
        }
        val query = ("[out:csv(::lat,::lon)];way(" + building.wayID + ");out center;")

        val client = OkHttpClient()

        val requestBody =
            ("data=" + URLEncoder.encode(
                query,
                StandardCharsets.UTF_8.toString()
            )).toRequestBody("application/x-www-form-urlencoded".toMediaType())

        val request = Request.Builder().url("https://overpass-api.de/api/interpreter?").post(requestBody).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use LatLng(0.0, 0.0)

                val bodyString = response.body?.string() ?: ""

                val lines = bodyString.split("\n")
                if (lines.size > 1) {
                    val addressLine = lines[1]
                    if (addressLine.isNotEmpty()) {
                        val parts = addressLine.split("\t")
                        if (parts.size >= 2) {
                            try {
                                return LatLng(parts[0].toDouble(), parts[1].toDouble())
                            }catch(e: NumberFormatException){
                                return LatLng(0.0, 0.0)
                            }
                        }
                    }
                    return LatLng(0.0, 0.0)
                }
            }
        return LatLng(0.0, 0.0)
    }

    suspend fun getPanorama(latLng: LatLng): Bitmap? {
        if(Math.abs(latLng.latitude+latLng.longitude) > 0.01){

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
                    return null
                }

                val bytes = response.body?.bytes()
                return bytes?.let {
                    BitmapFactory.decodeByteArray(it, 0, it.size)
                }
            }
        }
        return null
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

    fun focusOnCampus(campus: Campus, highlightedBuildingName: String? = null) {
        drawBuildings(campus, highlightedBuildingName)
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(campus.center, campus.defaultZoom))
    }

    fun updateHighlightsOnly(campus: Campus, highlightedBuildingName: String? = null) {
        drawBuildings(campus, highlightedBuildingName)
    }

    private var lastBuilding: String? = "NONE"

    private fun drawBuildings(campus: Campus, highlightedBuildingName: String?) {
        if (highlightedBuildingName == lastBuilding) {
            return
        }
        lastBuilding = highlightedBuildingName

        googleMap.clear()
        campus.buildings.forEach { building ->
            val isCurrentBuilding = building.name == highlightedBuildingName

            val polygon = PolygonOptions()
                .addAll(building.outline)
                .strokeWidth(if (isCurrentBuilding) 8f else 4f)
                .strokeColor(if (isCurrentBuilding)
                    "#FFD700".toColorInt() else //gold for when the user is near the building
                    "#912338".toColorInt())
                .fillColor(if (isCurrentBuilding)
                    Color.argb(80, 255, 204, 0) else
                    Color.argb(80, 145, 35, 56))

            googleMap.addPolygon(polygon)
        }
    }
}