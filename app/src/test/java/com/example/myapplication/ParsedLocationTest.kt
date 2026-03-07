package com.example.myapplication

import com.example.myapplication.data.LocationResult
import com.example.myapplication.data.ParsedLocation
import com.example.myapplication.logic.LocationResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for location string parsing.
 *
 * parseLocation() and the CAMPUS_* constants previously lived in ParsedLocation.kt.
 * They were moved to [LocationResolver] as part of the SRP refactor (PR #282).
 * A [BuildingNameProvider] lambda is injected so tests never load campuses.json.
 */
class ParsedLocationTest {

    private lateinit var resolver: LocationResolver

    private val fakeNames = mapOf(
        "H"  to "Henry F. Hall Building",
        "MB" to "John Molson Building",
        "EV" to "Engineering & Visual Arts",
        "HC" to "Hingston Hall",
        "LS" to "Learning Square"
    )

    @Before
    fun setUp() {
        // BuildingNameProvider is a fun interface — pass a lambda directly
        resolver = LocationResolver(buildingNames = { code -> fakeNames[code] })
    }

    // ── Online detection ──────────────────────────────────────────────────────

    @Test fun `online keyword returns Online`() {
        assertTrue(resolver.resolve("online") is LocationResult.Online)
    }

    @Test fun `remote keyword returns Online`() {
        assertTrue(resolver.resolve("remote") is LocationResult.Online)
    }

    @Test fun `zoom link returns Online`() {
        assertTrue(resolver.resolve("https://zoom.us/j/123456") is LocationResult.Online)
    }

    @Test fun `webex returns Online`() {
        assertTrue(resolver.resolve("webex meeting room") is LocationResult.Online)
    }

    // ── TBA / TBD detection ───────────────────────────────────────────────────

    @Test fun `TBA returns TBA`() {
        assertTrue(resolver.resolve("TBA") is LocationResult.TBA)
    }

    @Test fun `TBD returns TBA`() {
        assertTrue(resolver.resolve("TBD") is LocationResult.TBA)
    }

    @Test fun `to be announced returns TBA`() {
        assertTrue(resolver.resolve("to be announced") is LocationResult.TBA)
    }

    // ── Blank / unrecognisable ────────────────────────────────────────────────

    @Test fun `blank string returns Unknown`() {
        assertTrue(resolver.resolve("") is LocationResult.Unknown)
    }

    @Test fun `whitespace only returns Unknown`() {
        assertTrue(resolver.resolve("   ") is LocationResult.Unknown)
    }

    @Test fun `unrecognised string returns Unknown`() {
        assertTrue(resolver.resolve("some random text") is LocationResult.Unknown)
    }

    // ── Short format: "MB S1.401 SGW" ────────────────────────────────────────

    @Test fun `short SGW format parses correctly`() {
        val result = resolver.resolve("MB S1.401 SGW")
        assertTrue(result is LocationResult.Known)
        val loc = (result as LocationResult.Known).location
        assertEquals("MB", loc.buildingCode)
        assertEquals("John Molson Building", loc.buildingName)
        assertEquals("S1.401", loc.roomCode)
        assertEquals("Sir George Williams", loc.campus)
    }

    @Test fun `short LOY format parses correctly`() {
        val result = resolver.resolve("HC 101 LOY")
        assertTrue(result is LocationResult.Known)
        val loc = (result as LocationResult.Known).location
        assertEquals("HC", loc.buildingCode)
        assertEquals("Hingston Hall", loc.buildingName)
        assertEquals("101", loc.roomCode)
        assertEquals("Loyola", loc.campus)
    }

    @Test fun `single-letter building code parses correctly`() {
        val result = resolver.resolve("H 820 SGW")
        assertTrue(result is LocationResult.Known)
        val loc = (result as LocationResult.Known).location
        assertEquals("H", loc.buildingCode)
        assertEquals("Henry F. Hall Building", loc.buildingName)
        assertEquals("820", loc.roomCode)
        assertEquals("Sir George Williams", loc.campus)
    }

    // ── Long verbose format ───────────────────────────────────────────────────

    @Test fun `long SGW format parses correctly`() {
        // The resolver tokenises the string and looks for a known building CODE.
        // The string must contain the code ("H") as a standalone token — full
        // building names like "Hall Building" are not reverse-mapped to codes.
        val result = resolver.resolve("Sir George Williams Campus H Rm 862")
        assertTrue(result is LocationResult.Known)
        val loc = (result as LocationResult.Known).location
        assertEquals("H", loc.buildingCode)
        assertEquals("862", loc.roomCode)
        assertEquals("Sir George Williams", loc.campus)
    }

    @Test fun `long Loyola format parses correctly`() {
        // Same rule — include the building code "HC" as a standalone token.
        val result = resolver.resolve("Loyola Campus HC Rm 101")
        assertTrue(result is LocationResult.Known)
        val loc = (result as LocationResult.Known).location
        assertEquals("HC", loc.buildingCode)
        assertEquals("101", loc.roomCode)
        assertEquals("Loyola", loc.campus)
    }

    // ── roomDisplay and shortSummary helpers ──────────────────────────────────

    @Test fun `roomDisplay formats correctly`() {
        val loc = ParsedLocation(
            buildingCode = "H",
            buildingName = "Henry F. Hall Building",
            roomCode     = "820",
            campus       = "Sir George Williams"
        )
        assertEquals("H-820", loc.roomDisplay)
    }

    @Test fun `shortSummary formats correctly`() {
        val loc = ParsedLocation(
            buildingCode = "H",
            buildingName = "Henry F. Hall Building",
            roomCode     = "820",
            campus       = "Sir George Williams"
        )
        assertEquals(
            "Henry F. Hall Building · H-820 · Sir George Williams",
            loc.shortSummary
        )
    }
}
