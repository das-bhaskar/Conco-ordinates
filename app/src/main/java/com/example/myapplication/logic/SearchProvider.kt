package com.example.myapplication.logic

import com.example.myapplication.data.Building
import com.example.myapplication.data.Campus
import com.example.myapplication.data.CampusRepo
import com.example.myapplication.telemetry.CrashReporter
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.coroutines.tasks.await

sealed class SearchResult {
    data class BuildingResult(val building: Building) : SearchResult()
    data class CampusResult(val campus: Campus) : SearchResult()
    data class GoogleResult(val title: String, val address: String, val placeId: String) : SearchResult()
    object CurrentLocation : SearchResult()
    object Home : SearchResult()
}

class HybridSearchProvider(private val placesClient: PlacesClient) {
    suspend fun search(query: String): List<SearchResult> {
        if (query.isBlank()) return listOf(SearchResult.CurrentLocation, SearchResult.Home)

        val campusMatches = CampusRepo.getAllCampuses().filter { campus ->
            campus.name.contains(query, ignoreCase = true)
        }.map { SearchResult.CampusResult(it) }

        val allBuildings = CampusRepo.getAllCampuses().flatMap { it.buildings }
        val localMatches = allBuildings.filter { building ->
            building.name.contains(query, ignoreCase = true) ||
                    building.code.contains(query, ignoreCase = true)
        }.take(3).map { SearchResult.BuildingResult(it) }

        val request = FindAutocompletePredictionsRequest.builder().setQuery(query).build()
        return try {
            val response = placesClient.findAutocompletePredictions(request).await()
            val googleMatches = response.autocompletePredictions.map { prediction ->
                SearchResult.GoogleResult(
                    title = prediction.getPrimaryText(null).toString(),
                    address = prediction.getSecondaryText(null).toString(),
                    placeId = prediction.placeId
                )
            }
            campusMatches + localMatches + googleMatches
        } catch (e: Exception) {
            CrashReporter.setKey("search_query_length", query.length)
            CrashReporter.recordNonFatal(e, "places_autocomplete_failed")
            campusMatches + localMatches
        }
    }
}
