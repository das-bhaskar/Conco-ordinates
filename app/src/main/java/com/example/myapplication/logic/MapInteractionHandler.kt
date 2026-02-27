package com.example.myapplication.logic

import android.content.Context
import android.location.Geocoder
import com.google.android.gms.maps.model.LatLng
import com.example.myapplication.ui.viewmodel.MapViewModel
import java.util.Locale
import com.example.myapplication.BuildConfig
import com.example.myapplication.data.Building
import com.example.myapplication.telemetry.CrashReporter

object MapInteractionHandler {
    fun processClick(latLng: LatLng, viewModel: MapViewModel, context: Context) {
        val campus = viewModel.currentCampus ?: return
        val clickedBuildingName = MapManager.findBuildingAtLocation(latLng, campus)
        val buildingObject = campus.buildings.find { it.name == clickedBuildingName }

        if (buildingObject != null) {
            val streetAddress = getAddressFromCoords(context, latLng)
            viewModel.handleMapTap(buildingObject, streetAddress, null)

            val buildingCenter = buildingObject.getCenter()
            fetchProfessionalData(context, buildingCenter) { photoUrl ->
                viewModel.handleMapTap(buildingObject, streetAddress, photoUrl)
            }
        } else {
            viewModel.handleMapTap(null)
        }
    }

    private fun fetchProfessionalData(context: Context, latLng: LatLng, callback: (String?) -> Unit) {
        try {
            val apiKey = BuildConfig.MAPS_API_KEY
            if (apiKey == "DUMMY_KEY" || apiKey.isEmpty()) {
                callback(null)
                return
            }

            val streetViewUrl = "https://maps.googleapis.com/maps/api/streetview?" +
                    "size=600x300" +
                    "&location=${latLng.latitude},${latLng.longitude}" +
                    "&fov=90&heading=235&pitch=10" +
                    "&key=$apiKey"

            callback(streetViewUrl)
        } catch (e: Exception) {
            CrashReporter.setKey("interaction_context", "fetch_professional_data")
            CrashReporter.setKey("target_latlng", "${latLng.latitude},${latLng.longitude}")
            CrashReporter.recordNonFatal(e, "street_view_url_build_failed")
            callback(null)
        }
    }

    private fun getAddressFromCoords(context: Context, latLng: LatLng): String {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            addresses?.firstOrNull()?.getAddressLine(0) ?: "Address not found"
        } catch (e: Exception) {
            CrashReporter.setKey("interaction_context", "reverse_geocode")
            CrashReporter.setKey("target_latlng", "${latLng.latitude},${latLng.longitude}")
            CrashReporter.recordNonFatal(e, "reverse_geocode_failed")
            "Address unavailable"
        }
    }

    fun handleSearchSelection(building: Building, viewModel: MapViewModel, context: Context) {
        val center = building.getCenter()

        val realStreetAddress = getAddressFromCoords(context, center)

        viewModel.handleMapTap(building, realStreetAddress, null)

        fetchProfessionalData(context, center) { photoUrl ->
            viewModel.handleMapTap(building, realStreetAddress, photoUrl)
        }
    }

}

