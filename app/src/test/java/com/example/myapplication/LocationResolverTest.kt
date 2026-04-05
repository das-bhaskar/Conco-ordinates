package com.example.myapplication

import com.example.myapplication.data.BuildingNameProvider
import com.example.myapplication.data.LocationResult
import com.example.myapplication.data.ParsedLocation
import com.example.myapplication.logic.LocationResolver
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [LocationResolver].
 *
 * Uses a [FakeBuildingNameProvider] so tests run without CampusRepo / Android Context.
 */
class LocationResolverTest {

    /** Minimal fixture — only the codes the tests reference. */
    private class FakeBuildingNameProvider : BuildingNameProvider {
        private val map = mapOf(
            "MB" to "John Molson Building",
            "H"  to "Henry F. Hall Building",
            "EV" to "Engineering, CS and VA Integrated Complex",
            "CC" to "Central Building",
            "VL" to "Vanier Library"
        )
        override fun nameForCode(code: String): String? =
            map[code.uppercase()]
    }

    private lateinit var resolver: LocationResolver

    @Before
    fun setup() {
        resolver = LocationResolver(FakeBuildingNameProvider())
    }

    // ── null / blank ──────────────────────────────────────────────────────────

    @Test
    fun `resolve returns Unknown for null input`() {
        assertEquals(LocationResult.Unknown, resolver.resolve(null))
    }

    @Test
    fun `resolve returns Unknown for blank string`() {
        assertEquals(LocationResult.Unknown, resolver.resolve("   "))
    }

    @Test
    fun `resolve returns Unknown for empty string`() {
        assertEquals(LocationResult.Unknown, resolver.resolve(""))
    }

    // ── Online detection ──────────────────────────────────────────────────────

    @Test
    fun `resolve returns Online for literal online`() {
        assertEquals(LocationResult.Online, resolver.resolve("online"))
    }

    @Test
    fun `resolve returns Online for remote`() {
        assertEquals(LocationResult.Online, resolver.resolve("remote"))
    }

    @Test
    fun `resolve returns Online for string starting with Online`() {
        assertEquals(LocationResult.Online, resolver.resolve("Online via Zoom"))
    }

    @Test
    fun `resolve returns Online for zoom link`() {
        assertEquals(LocationResult.Online, resolver.resolve("Join via Zoom"))
    }

    @Test
    fun `resolve returns Online for webex link`() {
        assertEquals(LocationResult.Online, resolver.resolve("WebEx meeting"))
    }

    @Test
    fun `resolve returns Online for teams link`() {
        assertEquals(LocationResult.Online, resolver.resolve("Microsoft Teams"))
    }

    // ── TBA / TBD detection ──────────────────────────────────────────────────

    @Test
    fun `resolve returns TBA for tba`() {
        assertEquals(LocationResult.TBA, resolver.resolve("TBA"))
    }

    @Test
    fun `resolve returns TBA for tbd`() {
        assertEquals(LocationResult.TBA, resolver.resolve("TBD"))
    }

    @Test
    fun `resolve returns TBA for to be announced`() {
        assertEquals(LocationResult.TBA, resolver.resolve("to be announced"))
    }

    @Test
    fun `resolve returns TBA for to be determined`() {
        assertEquals(LocationResult.TBA, resolver.resolve("to be determined"))
    }

    // ── Short pattern: "MB S1.401 SGW" ───────────────────────────────────────

    @Test
    fun `resolve parses short SGW pattern`() {
        val result = resolver.resolve("MB S1.401 SGW")
        assertTrue(result is LocationResult.Known)
        val loc = (result as LocationResult.Known).location
        assertEquals("MB", loc.buildingCode)
        assertEquals("John Molson Building", loc.buildingName)
        assertEquals("S1.401", loc.roomCode)
        assertEquals("Sir George Williams", loc.campus)
    }

    @Test
    fun `resolve parses short LOY pattern`() {
        val result = resolver.resolve("CC 310 LOY")
        assertTrue(result is LocationResult.Known)
        val loc = (result as LocationResult.Known).location
        assertEquals("CC", loc.buildingCode)
        assertEquals("Central Building", loc.buildingName)
        assertEquals("310", loc.roomCode)
        assertEquals("Loyola", loc.campus)
    }

    @Test
    fun `resolve parses short EV pattern`() {
        val result = resolver.resolve("EV 3.309 EV")
        assertTrue(result is LocationResult.Known)
        val loc = (result as LocationResult.Known).location
        assertEquals("EV", loc.buildingCode)
        assertEquals("Engineering, CS and VA Integrated Complex", loc.buildingName)
        assertEquals("3.309", loc.roomCode)
    }

    @Test
    fun `resolve parses single letter building code`() {
        val result = resolver.resolve("H 535 SGW")
        assertTrue(result is LocationResult.Known)
        val loc = (result as LocationResult.Known).location
        assertEquals("H", loc.buildingCode)
        assertEquals("Henry F. Hall Building", loc.buildingName)
        assertEquals("535", loc.roomCode)
        assertEquals("Sir George Williams", loc.campus)
    }

    // ── Long / verbose pattern ───────────────────────────────────────────────

    @Test
    fun `resolve parses verbose SGW string with Rm notation`() {
        val result = resolver.resolve("Sir George Williams Campus - H Rm 862")
        assertTrue(result is LocationResult.Known)
        val loc = (result as LocationResult.Known).location
        assertEquals("H", loc.buildingCode)
        assertEquals("862", loc.roomCode)
        assertEquals("Sir George Williams", loc.campus)
    }

    @Test
    fun `resolve parses string with LOY campus mention`() {
        val result = resolver.resolve("Loyola Campus - VL Rm 101")
        assertTrue(result is LocationResult.Known)
        val loc = (result as LocationResult.Known).location
        assertEquals("VL", loc.buildingCode)
        assertEquals("101", loc.roomCode)
        assertEquals("Loyola", loc.campus)
    }

    @Test
    fun `resolve returns Unknown for unrecognised building in verbose format`() {
        val result = resolver.resolve("Sir George Williams Campus - ZZZ Rm 100")
        assertEquals(LocationResult.Unknown, result)
    }

    // ── parsedLocation convenience ───────────────────────────────────────────

    @Test
    fun `parsedLocation returns ParsedLocation for known input`() {
        val parsed = resolver.parsedLocation("H 535 SGW")
        assertNotNull(parsed)
        assertEquals("H", parsed!!.buildingCode)
    }

    @Test
    fun `parsedLocation returns null for online`() {
        assertNull(resolver.parsedLocation("online"))
    }

    @Test
    fun `parsedLocation returns null for null`() {
        assertNull(resolver.parsedLocation(null))
    }

    @Test
    fun `parsedLocation returns null for TBA`() {
        assertNull(resolver.parsedLocation("TBA"))
    }

    // ── Edge cases ───────────────────────────────────────────────────────────

    @Test
    fun `resolve trims whitespace before parsing`() {
        val result = resolver.resolve("  MB S1.401 SGW  ")
        assertTrue(result is LocationResult.Known)
    }

    @Test
    fun `resolve handles ONLINE in mixed case`() {
        assertEquals(LocationResult.Online, resolver.resolve("ONLINE"))
    }

    @Test
    fun `resolve handles TBA in mixed case`() {
        assertEquals(LocationResult.TBA, resolver.resolve("Tba"))
    }
}
