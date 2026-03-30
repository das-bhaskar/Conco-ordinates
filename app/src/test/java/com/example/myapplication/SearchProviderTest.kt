package com.example.myapplication.logic

import com.example.myapplication.data.Building
import com.example.myapplication.data.indoor.IndoorRepository
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
 * Scope: Tests that do NOT require Places API or Firebase.
 *
 * Not testable in JVM unit tests:
 * - searchIndoorRooms (requires Places await() → final class mock issue)
 * - Places API failure path (triggers CrashReporter → Firebase)
 * These are covered by Android instrumented tests.
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

    // ── Blank query (no Places API involved) ──────────────────────────────────

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
        assertTrue(results[1] is SearchResult.Home)
    }

    @Test
    fun `search blank query does not call placesClient`() = runTest {
        searchProvider.search("")
        verify(mockPlacesClient, never()).findAutocompletePredictions(any())
    }

    // ── SearchResult sealed class ─────────────────────────────────────────────

    @Test
    fun `SearchResult CurrentLocation is singleton`() {
        val a: SearchResult = SearchResult.CurrentLocation
        val b: SearchResult = SearchResult.CurrentLocation
        assertEquals(a, b)
        assertSame(a, b)
    }

    @Test
    fun `SearchResult Home is singleton`() {
        val a: SearchResult = SearchResult.Home
        val b: SearchResult = SearchResult.Home
        assertEquals(a, b)
        assertSame(a, b)
    }

    @Test
    fun `SearchResult IndoorRoomResult data class equality`() {
        val r1 = SearchResult.IndoorRoomResult("H", 8, "H-8-829", "node-829", "H-829 · H Floor 8")
        val r2 = r1.copy()
        assertEquals(r1, r2)
        assertEquals(r1.hashCode(), r2.hashCode())
        assertNotEquals(r1, r1.copy(floor = 1))
        assertTrue(r1.toString().contains("H-8-829"))
    }

    @Test
    fun `SearchResult IndoorRoomResult nodeId can be null`() {
        val r = SearchResult.IndoorRoomResult("H", 8, "H-8-829", null, "H-829")
        assertNull(r.nodeId)
    }

    @Test
    fun `SearchResult GoogleResult data class`() {
        val g = SearchResult.GoogleResult("Concordia Hall", "1455 De Maisonneuve", "place_abc")
        assertEquals("Concordia Hall", g.title)
        assertEquals("1455 De Maisonneuve", g.address)
        assertEquals("place_abc", g.placeId)
        val g2 = g.copy()
        assertEquals(g, g2)
        assertEquals(g.hashCode(), g2.hashCode())
    }

    @Test
    fun `SearchResult BuildingResult wraps building correctly`() {
        val building = Building("Hall Building", "H", 12345L,
            "1455 De Maisonneuve Blvd W", outline = null)
        val r = SearchResult.BuildingResult(building)
        assertEquals("H", r.building.code)
        assertEquals("Hall Building", r.building.name)
    }

    @Test
    fun `SearchResult CampusResult wraps campus correctly`() {
        val campus = com.example.myapplication.data.Campus(
            name      = "SGW Campus",
            center    = com.example.myapplication.data.JsonLatLng(45.495, -73.578),
            buildings = emptyList(),
            outline   = null
        )
        val r = SearchResult.CampusResult(campus)
        assertEquals("SGW Campus", r.campus.name)
    }

    @Test
    fun `HybridSearchProvider can be constructed with non-null indoorRepo`() {
        val provider = HybridSearchProvider(mockPlacesClient, mockIndoorRepo)
        assertNotNull(provider)
    }
}
