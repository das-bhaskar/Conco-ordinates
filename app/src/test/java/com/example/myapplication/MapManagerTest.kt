package com.example.myapplication.logic

import com.example.myapplication.data.Building
import com.example.myapplication.data.Campus
import com.example.myapplication.data.CampusRepo
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import kotlinx.coroutines.test.runTest

class MapManagerTest {

    private lateinit var mapManager: MapManager
    private val mockMap: GoogleMap = mock()

    @Before
    fun setup() {
        mapManager = MapManager(mockMap)
    }

    @Test
    fun `test checkClickBuildings when user click outside of a building`() {
        val result = mapManager.checkClickBuildings(LatLng(45.49591769537435, -73.57829009942627))
        assertEquals(null, result)
    }

    @Test
    fun `test checkClickBuildings when user click inside of a building`() {
        val result = mapManager.checkClickBuildings(LatLng(45.49719209795903, -73.57892542134059))
        assertNotNull(result)
        assertEquals("H", result!!.code)
    }

    @Test
    fun `test getCenterLatLng for a building`() = runTest() {
        var building = Building(
            "Henry F. Hall Building",
            "H",
            22080570,
            "1455 De Maisonneuve Blvd. W., Montreal, Quebec",
            listOf(
                LatLng(45.4968261, -73.5788241),
                LatLng(45.4970373, -73.5786245),
                LatLng(45.4972553, -73.5784091),
                LatLng(45.4973713, -73.5782939),
                LatLng(45.4974226, -73.5783991),
                LatLng(45.4975103, -73.5785786),
                LatLng(45.4977164, -73.5790075),
                LatLng(45.4977130, -73.5790121),
                LatLng(45.4974475, -73.5792690),
                LatLng(45.4971739, -73.5795378),
                LatLng(45.4971671, -73.5795431),
                LatLng(45.4971280, -73.5794591),
                LatLng(45.4968261, -73.5788241)
            )
        )
        var result = mapManager.getCenterLatLng(building)
        assertEquals(LatLng(45.4972712, -73.5789185), result)
    }

}