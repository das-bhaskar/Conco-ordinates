package com.example.myapplication

import com.example.myapplication.data.NearestStopResult
import com.example.myapplication.data.ShuttleAvailability
import com.example.myapplication.data.ShuttleStop
import com.example.myapplication.logic.ShuttleService
import com.google.android.gms.maps.model.LatLng
import java.util.Calendar

class MockShuttleService: ShuttleService {

    var availability: ShuttleAvailability = ShuttleAvailability.Active(nextDepartureMinutes = 10)
    var message: String = "Next shuttle in 10 minutes: 8:30"

    val stops = listOf(
        ShuttleStop(id = "SGW", name = "SGW", campus = "SGW", location = LatLng(45.4973, -73.5788)),
        ShuttleStop(id = "LOY", name = "LOY", campus = "Loyola", location = LatLng(45.4582, -73.6403))
    )

    override fun checkAvailability(
        fromCampus: String,
        calendar: Calendar
    ): ShuttleAvailability {
        return availability
    }

    override fun nearestStop(userLocation: LatLng?): NearestStopResult {
        return if (userLocation == null) {
            NearestStopResult.LocationUnavailable
        } else {
            NearestStopResult.Found(stops.first())
        }
    }

    override fun resolveNearestStop(userLocation: LatLng?): ShuttleStop? {
        return if (userLocation == null) {
            null
        }
        else {
            stops.first()
        }
    }

    override fun getAllStops(): List<ShuttleStop> {
        return stops
    }

    override fun statusMessage(
        fromCampus: String,
        calendar: Calendar
    ): String {
        return message
    }

}