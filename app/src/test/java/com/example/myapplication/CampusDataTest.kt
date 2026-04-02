package com.example.myapplication.data

import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for CampusRepo, Building, Campus, and related data classes.
 * Uses [CampusRepo.setTestCampuses] to inject test data without a Context.
 *
 * Note: Building has no `center` field — getCenter() computes centroid from outline.
 * If outline is null or empty, it falls back to LatLng(45.497, -73.579) (SGW).
 */
class CampusDataTest {

    // SGW campus rough polygon (clockwise)
    private val sgwPolygon = listOf(
        JsonLatLng(45.497, -73.582),
        JsonLatLng(45.497, -73.573),
        JsonLatLng(45.493, -73.573),
        JsonLatLng(45.493, -73.582)
    )

    // Building with a known outline so getCenter() is predictable
    private val hallBuilding = Building(
        name    = "Hall Building",
        code    = "H",
        wayID   = 12345L,
        address = "1455 De Maisonneuve Blvd W",
        outline = listOf(
            JsonLatLng(45.4970, -73.5792),
            JsonLatLng(45.4974, -73.5792),
            JsonLatLng(45.4974, -73.5788),
            JsonLatLng(45.4970, -73.5788)
        )
    )

    private val sgwCampus = Campus(
        name      = "SGW Campus",
        center    = JsonLatLng(45.495, -73.578),
        outline   = sgwPolygon,
        buildings = listOf(hallBuilding)
    )

    @Before
    fun setup() {
        CampusRepo.setTestCampuses(listOf(sgwCampus))
    }

    // ── CampusRepo.getAllCampuses ──────────────────────────────────────────────

    @Test
    fun `getAllCampuses returns injected campuses`() {
        val all = CampusRepo.getAllCampuses()
        assertEquals(1, all.size)
        assertEquals("SGW Campus", all.first().name)
    }

    // ── CampusRepo.getCampusByName ────────────────────────────────────────────

    @Test
    fun `getCampusByName returns campus for exact name`() {
        val campus = CampusRepo.getCampusByName("SGW Campus")
        assertNotNull(campus)
        assertEquals("SGW Campus", campus!!.name)
    }

    @Test
    fun `getCampusByName returns null for unknown name`() {
        assertNull(CampusRepo.getCampusByName("Nonexistent Campus"))
    }

    // ── CampusRepo.getCampus (point-in-polygon) ───────────────────────────────

    @Test
    fun `getCampus returns campus when point is inside polygon`() {
        val inside = LatLng(45.495, -73.578)
        val campus = CampusRepo.getCampus(inside)
        assertNotNull(campus)
        assertEquals("SGW Campus", campus!!.name)
    }

    @Test
    fun `getCampus returns null when point is outside all polygons`() {
        val outside = LatLng(40.000, -70.000)
        assertNull(CampusRepo.getCampus(outside))
    }

    @Test
    fun `getCampus returns null when campus outline is empty`() {
        // Empty outline means isInsidePolygon never matches.
        // Use a point very far from center to also avoid the proximity fallback (< 0.005 deg).
        val emptyOutlineCampus = Campus(
            name      = "Empty",
            center    = JsonLatLng(10.0, 10.0),   // center far from test point
            outline   = emptyList(),
            buildings = emptyList()
        )
        CampusRepo.setTestCampuses(listOf(emptyOutlineCampus))
        // Test point is 1 degree away from center — outside 0.005 deg fallback zone
        assertNull(CampusRepo.getCampus(LatLng(11.0, 11.0)))
    }

    // ── Building.getCenter ────────────────────────────────────────────────────

    @Test
    fun `Building getCenter returns centroid of outline polygon`() {
        // Outline is a square: avg lat = 45.4972, avg lng = -73.5790
        val center = hallBuilding.getCenter()
        assertEquals(45.4972, center.latitude, 0.0001)
        assertEquals(-73.5790, center.longitude, 0.0001)
    }

    @Test
    fun `Building getCenter falls back to SGW when outline is null`() {
        val b = Building("Test", "T", 0L, "Addr", outline = null)
        val c = b.getCenter()
        // Fallback is hardcoded LatLng(45.497, -73.579)
        assertEquals(45.497, c.latitude, 0.001)
        assertEquals(-73.579, c.longitude, 0.001)
    }

    @Test
    fun `Building getCenter falls back to SGW when outline is empty`() {
        val b = Building("Test", "T", 0L, "Addr", outline = emptyList())
        val c = b.getCenter()
        assertEquals(45.497, c.latitude, 0.001)
    }

    // ── Building.getGoogleOutline ─────────────────────────────────────────────

    @Test
    fun `Building getGoogleOutline converts JsonLatLng list to LatLng list`() {
        val outline = hallBuilding.getGoogleOutline()
        assertEquals(4, outline.size)
        assertEquals(45.4970, outline.first().latitude, 0.0001)
    }

    @Test
    fun `Building getGoogleOutline returns empty list when outline is null`() {
        val b = Building("Test", "T", 0L, "Addr", outline = null)
        assertTrue(b.getGoogleOutline().isEmpty())
    }

    @Test
    fun `Building data class equality and copy`() {
        val b2 = hallBuilding.copy()
        assertEquals(hallBuilding, b2)
        assertNotEquals(hallBuilding, hallBuilding.copy(code = "CC"))
        assertEquals(hallBuilding.hashCode(), b2.hashCode())
    }

    @Test
    fun `Building default isCampusBuilding is true`() {
        assertTrue(hallBuilding.isCampusBuilding)
    }

    // ── Campus ────────────────────────────────────────────────────────────────

    @Test
    fun `Campus getGoogleCenter returns LatLng from center field`() {
        val center = sgwCampus.getGoogleCenter()
        assertEquals(45.495, center.latitude, 0.0001)
        assertEquals(-73.578, center.longitude, 0.0001)
    }

    @Test
    fun `Campus getGoogleOutline converts all polygon points`() {
        val outline = sgwCampus.getGoogleOutline()
        assertEquals(4, outline.size)
    }

    @Test
    fun `Campus getGoogleOutline returns empty for null outline`() {
        val c = sgwCampus.copy(outline = null)
        assertTrue(c.getGoogleOutline().isEmpty())
    }

    @Test
    fun `Campus data class equality`() {
        val c2 = sgwCampus.copy()
        assertEquals(sgwCampus, c2)
        assertNotEquals(sgwCampus, sgwCampus.copy(name = "Other"))
    }

    // ── JsonLatLng ────────────────────────────────────────────────────────────

    @Test
    fun `JsonLatLng equality copy and hashCode`() {
        val j1 = JsonLatLng(45.495, -73.578)
        val j2 = j1.copy()
        assertEquals(j1, j2)
        assertEquals(j1.hashCode(), j2.hashCode())
        assertNotEquals(j1, j1.copy(latitude = 0.0))
        assertTrue(j1.toString().contains("45.495"))
    }
}
