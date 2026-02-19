package com.example.myapplication

import com.example.myapplication.logic.MapManager
import com.example.myapplication.data.Campus
import com.example.myapplication.data.Building
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class ClosestBuildingTest {

    private lateinit var mapManager: MapManager
    private lateinit var campus: Campus

    @Before
    fun setup() {
        val googleMap = mock(GoogleMap::class.java)
        mapManager = MapManager(googleMap)

        val buildingA = Building(
            name = "Building A",
            code = "A001",
            wayID = 1L,
            "1455 De Maisonneuve Blvd. W., Montreal, Quebec",
            outline = listOf(
                LatLng(45.0, -73.0),
                LatLng(45.0, -73.001),
                LatLng(45.001, -73.001),
                LatLng(45.001, -73.0)
            )
        )

        val buildingB = Building(
            name = "Building B",
            code = "B001",
            wayID = 2L,
            "1455 De Maisonneuve Blvd. W., Montreal, Quebec",
            outline = listOf(
                LatLng(45.002, -73.002),
                LatLng(45.002, -73.003),
                LatLng(45.003, -73.003),
                LatLng(45.003, -73.002)
            )
        )


        campus = Campus(
            name = "Test Campus",
            center = LatLng(45.0, -73.0),
            defaultZoom = 18f,
            buildings = listOf(buildingA, buildingB)
        )
    }

    @Test
    fun userInsideBuilding_returnsBuildingName() {
        val userLocation = LatLng(45.0005, -73.0005)
        val result = mapManager.findBuildingAtLocation(userLocation, campus)
        assertEquals("Building A", result)
    }

    @Test
    fun userNearBuildingWithin10Meters_returnsClosestBuilding() {
        val userLocation = LatLng(45.00105, -73.0005) // just outside building A
        val result = mapManager.findBuildingAtLocation(userLocation, campus)
        assertEquals("Building A", result)
    }

    @Test
    fun userNearBuildingBWithin10Meters_returnsClosestBuildingB() {
        val userLocation = LatLng(45.0021, -73.0025) // just outside building B
        val result = mapManager.findBuildingAtLocation(userLocation, campus)
        assertEquals("Building B", result)
    }

    @Test
    fun userFarFromAllBuildings_returnsNull() {
        val userLocation = LatLng(46.0, -74.0) // far away
        val result = mapManager.findBuildingAtLocation(userLocation, campus)
        assertNull(result)
    }
}
