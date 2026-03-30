package com.example.myapplication.logic

import androidx.compose.ui.geometry.Offset
import com.example.myapplication.data.Building
import com.example.myapplication.data.Campus
import com.example.myapplication.data.JsonLatLng
import com.example.myapplication.data.indoor.IndoorFloor
import com.example.myapplication.data.indoor.IndoorNode
import com.example.myapplication.data.indoor.IndoorRepository
import com.example.myapplication.data.indoor.IndoorRoom
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

/**
 * Unit tests for [HybridSearchProvider] and [SearchResult].
 *
 * Strategy:
 * - Blank query tests: no Places API needed.
 * - Indoor room search: call [HybridSearchProvider.searchIndoorRooms] directly
 *   (now internal @VisibleForTesting) to bypass Places API entirely.
 * - [SearchResult] sealed subclasses: pure data class tests.
 * - [floorsFor]: direct unit test.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SearchProviderTest {

    private lateinit var mockPlacesClient: PlacesClient
    private lateinit var mockIndoorRepo: IndoorRepository
    private lateinit var searchProvider: HybridSearchProvider

    @Before
    fun setup() {
        com.example.myapplication.telemetry.CrashReporter.isTesting = true
        mockPlacesClient = mock()
        mockIndoorRepo   = mock()
        searchProvider   = HybridSearchProvider(mockPlacesClient, mockIndoorRepo)
    }

    // ── Blank query ───────────────────────────────────────────────────────────

    @Test
    fun `search returns CurrentLocation and Home when query is blank`() = runTest {
        val results = searchProvider.search("")
        assertEquals(2, results.size)
        assertTrue(results.any { it is SearchResult.CurrentLocation })
        assertTrue(results.any { it is SearchResult.Home })
    }

    @Test
    fun `search returns defaults for whitespace-only query`() = runTest {
        val results = searchProvider.search("   ")
        assertEquals(2, results.size)
        assertTrue(results[0] is SearchResult.CurrentLocation)
    }

    @Test
    fun `search blank query does not call placesClient`() = runTest {
        searchProvider.search("")
        verify(mockPlacesClient, never()).findAutocompletePredictions(any())
    }

    // ── searchIndoorRooms (internal — called directly to avoid Places API) ────

    @Test
    fun `searchIndoorRooms returns IndoorRoomResult for valid H-829 query`() = runTest {
        val room = IndoorRoom("H-8-829", "classroom", "H-829",
            polygon = listOf(Offset(0.5f, 0.5f)))
        val node = IndoorNode("node-829", 0.5f, 0.5f, "ROOM", roomId = "H-8-829")
        val floorData = IndoorFloor("H", 8, rooms = listOf(room), nodes = listOf(node))

        whenever(mockIndoorRepo.getFloor(any(), any())).thenReturn(null)
        whenever(mockIndoorRepo.getFloor("H", 8)).thenReturn(floorData)

        val results = searchProvider.searchIndoorRooms("H-829")
        assertTrue(results.isNotEmpty())
        assertEquals("H", results.first().buildingCode)
        assertEquals(8, results.first().floor)
        assertEquals("H-8-829", results.first().roomId)
    }

    @Test
    fun `searchIndoorRooms returns results from multiple floors`() = runTest {
        val room1 = IndoorRoom("H-1-110", "classroom", "H-110",
            polygon = listOf(Offset(0.5f, 0.5f)))
        val node1 = IndoorNode("node-110-f1", 0.5f, 0.5f, "ROOM", roomId = "H-1-110")
        val floor1 = IndoorFloor("H", 1, rooms = listOf(room1), nodes = listOf(node1))

        val room8 = IndoorRoom("H-8-110", "classroom", "H-110",
            polygon = listOf(Offset(0.5f, 0.5f)))
        val node8 = IndoorNode("node-110-f8", 0.5f, 0.5f, "ROOM", roomId = "H-8-110")
        val floor8 = IndoorFloor("H", 8, rooms = listOf(room8), nodes = listOf(node8))

        whenever(mockIndoorRepo.getFloor(any(), any())).thenReturn(null)
        whenever(mockIndoorRepo.getFloor("H", 1)).thenReturn(floor1)
        whenever(mockIndoorRepo.getFloor("H", 8)).thenReturn(floor8)

        val results = searchProvider.searchIndoorRooms("H-110")
        assertTrue(results.size >= 2)
        assertTrue(results.any { it.floor == 1 })
        assertTrue(results.any { it.floor == 8 })
    }

    @Test
    fun `searchIndoorRooms returns empty for unknown building code`() = runTest {
        val results = searchProvider.searchIndoorRooms("ZZ-101")
        assertTrue(results.isEmpty())
    }

    @Test
    fun `searchIndoorRooms returns empty for pure digit query`() = runTest {
        val results = searchProvider.searchIndoorRooms("829829")
        assertTrue(results.isEmpty())
    }

    @Test
    fun `searchIndoorRooms returns empty when repo has no floor data`() = runTest {
        whenever(mockIndoorRepo.getFloor(any(), any())).thenReturn(null)
        val results = searchProvider.searchIndoorRooms("H-829")
        assertTrue(results.isEmpty())
    }

    @Test
    fun `searchIndoorRooms caps results at 3`() = runTest {
        // Create 5 matching rooms on different floors
        val makeFloor = { floor: Int ->
            val room = IndoorRoom("H-$floor-110", "classroom", "H-110",
                polygon = listOf(Offset(0.5f, 0.5f)))
            val node = IndoorNode("node-$floor", 0.5f, 0.5f, "ROOM", roomId = "H-$floor-110")
            IndoorFloor("H", floor, rooms = listOf(room), nodes = listOf(node))
        }
        whenever(mockIndoorRepo.getFloor(any(), any())).thenReturn(null)
        whenever(mockIndoorRepo.getFloor("H", 1)).thenReturn(makeFloor(1))
        whenever(mockIndoorRepo.getFloor("H", 2)).thenReturn(makeFloor(2))
        whenever(mockIndoorRepo.getFloor("H", 8)).thenReturn(makeFloor(8))
        whenever(mockIndoorRepo.getFloor("H", 9)).thenReturn(makeFloor(9))

        val results = searchProvider.searchIndoorRooms("H-110")
        assertTrue(results.size <= 3)
    }

    @Test
    fun `searchIndoorRooms result includes node id when node linked to room`() = runTest {
        val room = IndoorRoom("H-8-829", "classroom", "H-829",
            polygon = listOf(Offset(0.5f, 0.5f)))
        val node = IndoorNode("node-abc", 0.5f, 0.5f, "ROOM", roomId = "H-8-829")
        val floor = IndoorFloor("H", 8, rooms = listOf(room), nodes = listOf(node))

        whenever(mockIndoorRepo.getFloor(any(), any())).thenReturn(null)
        whenever(mockIndoorRepo.getFloor("H", 8)).thenReturn(floor)

        val results = searchProvider.searchIndoorRooms("H-829")
        assertEquals("node-abc", results.first().nodeId)
    }

    @Test
    fun `searchIndoorRooms result nodeId is null when no node linked`() = runTest {
        val room = IndoorRoom("H-8-829", "classroom", "H-829",
            polygon = listOf(Offset(0.5f, 0.5f)))
        // No node with matching roomId
        val node = IndoorNode("node-xyz", 0.5f, 0.5f, "ROOM", roomId = null)
        val floor = IndoorFloor("H", 8, rooms = listOf(room), nodes = listOf(node))

        whenever(mockIndoorRepo.getFloor(any(), any())).thenReturn(null)
        whenever(mockIndoorRepo.getFloor("H", 8)).thenReturn(floor)

        val results = searchProvider.searchIndoorRooms("H-829")
        assertTrue(results.isEmpty() || results.first().nodeId == null)
    }

    // ── floorsFor (internal) ──────────────────────────────────────────────────

    @Test
    fun `floorsFor returns correct floors for H`() {
        assertEquals(listOf(1, 2, 8, 9), searchProvider.floorsFor("H"))
    }

    @Test
    fun `floorsFor returns correct floors for CC`() {
        assertEquals(listOf(1), searchProvider.floorsFor("CC"))
    }

    @Test
    fun `floorsFor returns correct floors for MB including basement`() {
        val floors = searchProvider.floorsFor("MB")
        assertTrue(floors.contains(-2))
        assertTrue(floors.contains(1))
    }

    @Test
    fun `floorsFor returns empty for unknown building`() {
        assertTrue(searchProvider.floorsFor("ZZ").isEmpty())
    }

    // ── SearchResult sealed class ─────────────────────────────────────────────

    @Test
    fun `SearchResult CurrentLocation is singleton`() {
        assertSame(SearchResult.CurrentLocation, SearchResult.CurrentLocation)
    }

    @Test
    fun `SearchResult Home is singleton`() {
        assertSame(SearchResult.Home, SearchResult.Home)
    }

    @Test
    fun `SearchResult IndoorRoomResult data class equality`() {
        val r1 = SearchResult.IndoorRoomResult("H", 8, "H-8-829", "node-829", "H-829 · H Floor 8")
        val r2 = r1.copy()
        assertEquals(r1, r2)
        assertEquals(r1.hashCode(), r2.hashCode())
        assertNotEquals(r1, r1.copy(floor = 1))
        assertNull(r1.copy(nodeId = null).nodeId)
    }

    @Test
    fun `SearchResult GoogleResult data class`() {
        val g = SearchResult.GoogleResult("Hall", "1455 De Maisonneuve", "place_abc")
        assertEquals("Hall", g.title)
        val g2 = g.copy()
        assertEquals(g, g2)
    }

    @Test
    fun `SearchResult BuildingResult wraps building correctly`() {
        val building = Building("Hall Building", "H", 12345L,
            "1455 De Maisonneuve Blvd W", outline = null)
        val r = SearchResult.BuildingResult(building)
        assertEquals("H", r.building.code)
    }

    @Test
    fun `SearchResult CampusResult wraps campus correctly`() {
        val campus = Campus("SGW Campus", JsonLatLng(45.495, -73.578),
            emptyList(), null)
        val r = SearchResult.CampusResult(campus)
        assertEquals("SGW Campus", r.campus.name)
    }

    @Test
    fun `HybridSearchProvider can be constructed with non-null indoorRepo`() {
        assertNotNull(HybridSearchProvider(mockPlacesClient, mockIndoorRepo))
    }
}
