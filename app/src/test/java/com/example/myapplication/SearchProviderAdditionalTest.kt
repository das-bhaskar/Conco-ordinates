package com.example.myapplication.logic

import androidx.compose.ui.geometry.Offset
import com.example.myapplication.data.Building
import com.example.myapplication.data.Campus
import com.example.myapplication.data.JsonLatLng
import com.example.myapplication.data.indoor.IIndoorRepository
import com.example.myapplication.data.indoor.IndoorFloor
import com.example.myapplication.data.indoor.IndoorNode
import com.example.myapplication.data.indoor.IndoorRoom
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class SearchProviderAdditionalTest {

    private lateinit var placesClient: PlacesClient
    private lateinit var indoorRepo: IIndoorRepository
    private lateinit var provider: HybridSearchProvider

    private val hall = Building(
        name = "Henry F. Hall Building",
        code = "H",
        wayID = 1L,
        address = "1455 De Maisonneuve",
        outline = listOf(
            JsonLatLng(45.496, -73.580),
            JsonLatLng(45.498, -73.580),
            JsonLatLng(45.498, -73.578),
            JsonLatLng(45.496, -73.578)
        )
    )

    @Before
    fun setup() {
        placesClient = mock()
        indoorRepo = mock()
        provider = HybridSearchProvider(placesClient, indoorRepo)
    }

    @Test
    fun `searchIndoorRooms supports compact code format`() = runTest {
        val room = IndoorRoom("H-8-829", "classroom", "H-829", polygon = listOf(Offset(0.5f, 0.5f)))
        val node = IndoorNode("node-829", 0.5f, 0.5f, "ROOM", roomId = "H-8-829")
        whenever(indoorRepo.getFloor(any(), any())).thenReturn(null)
        whenever(indoorRepo.getFloor("H", 8)).thenReturn(IndoorFloor("H", 8, rooms = listOf(room), nodes = listOf(node)))

        val results = provider.searchIndoorRooms("H829")

        assertEquals(1, results.size)
        assertEquals("node-829", results.first().nodeId)
    }

    @Test
    fun `searchIndoorRooms supports lowercase with spaces`() = runTest {
        val room = IndoorRoom("H-8-829", "classroom", "H-829", polygon = listOf(Offset(0.5f, 0.5f)))
        val node = IndoorNode("node-829", 0.5f, 0.5f, "ROOM", roomId = "H-8-829")
        whenever(indoorRepo.getFloor(any(), any())).thenReturn(null)
        whenever(indoorRepo.getFloor("H", 8)).thenReturn(IndoorFloor("H", 8, rooms = listOf(room), nodes = listOf(node)))

        val results = provider.searchIndoorRooms("h 829")

        assertEquals(1, results.size)
        assertEquals("H", results.first().buildingCode)
    }

    @Test
    fun `displayName and coordinates cover all SearchResult variants`() {
        val campus = Campus("SGW Campus", JsonLatLng(45.497, -73.579), listOf(hall), null)
        val current = LatLng(45.5, -73.6)

        assertEquals("Henry F. Hall Building", SearchResult.BuildingResult(hall).displayName)
        assertEquals(hall.getCenter(), SearchResult.BuildingResult(hall).coordinates(null))
        assertEquals("SGW Campus", SearchResult.CampusResult(campus).displayName)
        assertEquals(hall.getCenter(), SearchResult.CampusResult(campus).coordinates(null))
        assertEquals("Hall", SearchResult.GoogleResult("Hall", "Addr", "id").displayName)
        assertNull(SearchResult.GoogleResult("Hall", "Addr", "id").coordinates(null))
        assertEquals("Your position", SearchResult.CurrentLocation.displayName)
        assertEquals(current, SearchResult.CurrentLocation.coordinates(current))
        assertEquals("Home", SearchResult.Home.displayName)
        assertEquals(LatLng(45.51723868665001, -73.627297124046), SearchResult.Home.coordinates(null))
        assertEquals("H-829", SearchResult.IndoorRoomResult("H", 8, "H-8-829", "node", "H-829").displayName)
        assertNull(SearchResult.IndoorRoomResult("H", 8, "H-8-829", "node", "H-829").coordinates(null))
    }

    @Test
    fun `campus result coordinates returns null when campus has no buildings`() {
        val campus = Campus("Empty", JsonLatLng(45.0, -73.0), emptyList(), null)
        assertNull(SearchResult.CampusResult(campus).coordinates(null))
    }
}
