package com.example.myapplication.data

import android.content.Context
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.example.myapplication.R
import com.example.myapplication.telemetry.CrashReporter
import kotlin.math.pow
import kotlin.math.sqrt

data class JsonLatLng(val latitude: Double, val longitude: Double)

data class Building(
    val name: String,
    val code: String,
    val wayID: Long,
    val address: String,
    val outline: List<JsonLatLng>?, // Make nullable for safety
    val isCampusBuilding: Boolean = true
) {
    fun getGoogleOutline(): List<LatLng> = outline?.map { LatLng(it.latitude, it.longitude) } ?: emptyList()

    fun getCenter(): LatLng {
        val points = getGoogleOutline()
        if (points.isEmpty()) return LatLng(45.497, -73.579) // Fallback to SGW center

        val avgLat = points.map { it.latitude }.average()
        val avgLng = points.map { it.longitude }.average()
        return LatLng(avgLat, avgLng)
    }
}

data class Campus(
    val name: String,
    val center: JsonLatLng,
    val buildings: List<Building>,
    val outline: List<JsonLatLng>?, // Make nullable for safety
    val defaultZoom: Float = 17f
) {
    fun getGoogleCenter(): LatLng = LatLng(center.latitude, center.longitude)
    fun getGoogleOutline(): List<LatLng> = outline?.map { LatLng(it.latitude, it.longitude) } ?: emptyList()
}

data class CampusDataWrapper(val campuses: List<Campus>)

object CampusRepo {
    private var allCampuses: List<Campus> = emptyList()

    fun initialize(context: Context) {
        android.util.Log.d("DATA_CHECK", "1. Initialize started (using res/raw)")
        try {
            val inputStream = context.resources.openRawResource(R.raw.campuses)
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            android.util.Log.d("DATA_CHECK", "2. JSON read success. Length: ${jsonString.length}")

            val wrapper = Gson().fromJson(jsonString, CampusDataWrapper::class.java)

            if (wrapper != null) {
                allCampuses = wrapper.campuses
                android.util.Log.d("DATA_CHECK", "3. Success! Loaded ${allCampuses.size} campuses")
            } else {
                android.util.Log.e("DATA_CHECK", "3. ERROR: Gson returned null wrapper")
            }
        } catch (e: Exception) {
            android.util.Log.e("DATA_CHECK", "FATAL ERROR: ${e.message}")
            CrashReporter.setKey("campus_data_source", "res/raw/campuses.json")
            CrashReporter.recordNonFatal(e, "campus_data_initialize_failed")
            e.printStackTrace()
        }
    }

    fun getCampus(point: LatLng): Campus? {
        // 1. Strict Check: Is the user physically inside the campus perimeter?
        val insideCampus = allCampuses.find { campus ->
            val outline = campus.getGoogleOutline()
            outline.isNotEmpty() && isInsidePolygon(point, outline)
        }
        if (insideCampus != null) return insideCampus

        // 2. Strict Check: Is the user inside a specific building?
        val insideBuilding = allCampuses.find { campus ->
            campus.buildings.any { building ->
                isInsidePolygon(point, building.getGoogleOutline())
            }
        }
        if (insideBuilding != null) return insideBuilding

        // 3. Conditional Fallback: Only return the nearest if within a logical "Campus Zone".
        // Note: uses Euclidean degree distance as an approximation — accurate enough for
        // the ~1 km campus zones involved, without the overhead of a full haversine calc.
        val maxDistanceDegrees = 0.005

        return allCampuses.filter { campus ->
            val center = campus.getGoogleCenter()
            val dist = sqrt(
                (point.latitude  - center.latitude).pow(2.0) +
                (point.longitude - center.longitude).pow(2.0)
            )
            dist < maxDistanceDegrees
        }.minByOrNull { campus ->
            val center = campus.getGoogleCenter()
            sqrt(
                (point.latitude  - center.latitude).pow(2.0) +
                (point.longitude - center.longitude).pow(2.0)
            )
        }
    }

    fun getCampusByName(name: String): Campus? = allCampuses.find {
        it.name.trim().equals(name.trim(), ignoreCase = true)
    }

    private fun isInsidePolygon(point: LatLng, polygon: List<LatLng>): Boolean {
        if (polygon.isEmpty()) return false
        var intersectCount = 0
        for (j in polygon.indices) {
            val i = if (j == 0) polygon.size - 1 else j - 1
            if (rayCastIntersect(point, polygon[i], polygon[j])) intersectCount++
        }
        return intersectCount % 2 != 0
    }

    fun setTestCampuses(campuses: List<Campus>) {
        allCampuses = campuses
    }

    private fun rayCastIntersect(tap: LatLng, vertA: LatLng, vertB: LatLng): Boolean {
        val aY = vertA.latitude;  val bY = vertB.latitude
        val aX = vertA.longitude; val bX = vertB.longitude
        val pY = tap.latitude;    val pX = tap.longitude

        // Degenerate edges: horizontal lines cause division-by-zero in slope calc;
        // vertical lines (aX == bX) produce an infinite slope — skip both.
        if (aY == bY || aX == bX) return false

        if ((aY > pY && bY > pY) || (aY < pY && bY < pY) || (aX < pX && bX < pX)) return false

        val m            = (aY - bY) / (aX - bX)
        val bee          = aY - m * aX
        val xIntersection = (pY - bee) / m
        return xIntersection > pX
    }

    fun getAllCampuses(): List<Campus> = allCampuses
}
