package com.example.myapplication.data

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for [parseLocation].
 *
 * Pure function, zero dependencies — no Android context required.
 * Covers every [LocationResult] branch.
 */
class ParsedLocationTest {

    // ── LocationResult.Unknown ────────────────────────────────────────────────

    @Test
    fun `blank string returns Unknown`() {
        assertEquals(LocationResult.Unknown, parseLocation(""))
    }

    @Test
    fun `whitespace-only string returns Unknown`() {
        assertEquals(LocationResult.Unknown, parseLocation("   "))
    }

    @Test
    fun `unrecognised text returns Unknown`() {
        assertEquals(LocationResult.Unknown, parseLocation("Some random place"))
    }

    // ── LocationResult.Online ─────────────────────────────────────────────────

    @Test
    fun `exact 'online' returns Online`() {
        assertEquals(LocationResult.Online, parseLocation("online"))
    }

    @Test
    fun `exact 'Online' case-insensitive returns Online`() {
        assertEquals(LocationResult.Online, parseLocation("Online"))
    }

    @Test
    fun `exact 'remote' returns Online`() {
        assertEquals(LocationResult.Online, parseLocation("remote"))
    }

    @Test
    fun `string starting with 'online' returns Online`() {
        assertEquals(LocationResult.Online, parseLocation("Online - Zoom link in Moodle"))
    }

    @Test
    fun `string containing 'zoom' returns Online`() {
        assertEquals(LocationResult.Online, parseLocation("https://concordia.zoom.us/j/12345"))
    }

    @Test
    fun `string containing 'webex' returns Online`() {
        assertEquals(LocationResult.Online, parseLocation("WebEx meeting"))
    }

    @Test
    fun `string containing 'teams' returns Online`() {
        assertEquals(LocationResult.Online, parseLocation("Microsoft Teams"))
    }

    // ── LocationResult.TBA ────────────────────────────────────────────────────

    @Test
    fun `exact 'tba' returns TBA`() {
        assertEquals(LocationResult.TBA, parseLocation("tba"))
    }

    @Test
    fun `exact 'TBA' returns TBA`() {
        assertEquals(LocationResult.TBA, parseLocation("TBA"))
    }

    @Test
    fun `exact 'tbd' returns TBA`() {
        assertEquals(LocationResult.TBA, parseLocation("tbd"))
    }

    @Test
    fun `exact 'TBD' returns TBA`() {
        assertEquals(LocationResult.TBA, parseLocation("TBD"))
    }

    @Test
    fun `'to be announced' returns TBA`() {
        assertEquals(LocationResult.TBA, parseLocation("to be announced"))
    }

    @Test
    fun `'to be determined' returns TBA`() {
        assertEquals(LocationResult.TBA, parseLocation("to be determined"))
    }

    // ── LocationResult.Known — short pattern ──────────────────────────────────

    @Test
    fun `short pattern SGW campus parses correctly`() {
        val result = parseLocation("H 820 SGW")
        assertTrue(result is LocationResult.Known)
        val loc = (result as LocationResult.Known).location
        assertEquals("H",                   loc.buildingCode)
        assertEquals("Henry F. Hall",       loc.buildingName)
        assertEquals("820",                 loc.roomCode)
        assertEquals(CAMPUS_SGW,            loc.campus)
    }

    @Test
    fun `short pattern LOY campus parses correctly`() {
        val result = parseLocation("HC 101 LOY")
        assertTrue(result is LocationResult.Known)
        val loc = (result as LocationResult.Known).location
        assertEquals("HC",          loc.buildingCode)
        assertEquals(CAMPUS_LOYOLA, loc.campus)
    }

    @Test
    fun `short pattern MB with room dot notation parses correctly`() {
        val result = parseLocation("MB S1.401 SGW")
        assertTrue(result is LocationResult.Known)
        val loc = (result as LocationResult.Known).location
        assertEquals("MB",                    loc.buildingCode)
        assertEquals("John Molson Building",  loc.buildingName)
        assertEquals("S1.401",               loc.roomCode)
        assertEquals(CAMPUS_SGW,              loc.campus)
    }

    @Test
    fun `roomDisplay is formatted as buildingCode-roomCode`() {
        val result = parseLocation("H 535 SGW") as LocationResult.Known
        assertEquals("H-535", result.location.roomDisplay)
    }

    // ── LocationResult.Known — long pattern ───────────────────────────────────

    @Test
    fun `long SGW verbose string with building code parses correctly`() {
        // Long pattern works when the building CODE appears in the string
        val result = parseLocation("Sir George Williams Campus H Rm 862")
        assertTrue(result is LocationResult.Known)
        val loc = (result as LocationResult.Known).location
        assertEquals("H",        loc.buildingCode)
        assertEquals("862",      loc.roomCode)
        assertEquals(CAMPUS_SGW, loc.campus)
    }

    @Test
    fun `long Loyola verbose string with building code parses correctly`() {
        // Long pattern works when the building CODE appears in the string
        val result = parseLocation("Loyola Campus HC Rm 210")
        assertTrue(result is LocationResult.Known)
        val loc = (result as LocationResult.Known).location
        assertEquals("HC",           loc.buildingCode)
        assertEquals("210",          loc.roomCode)
        assertEquals(CAMPUS_LOYOLA,  loc.campus)
    }

    @Test
    fun `long verbose string without recognisable code returns Unknown`() {
        // Building full names like "Hall Building" are not in the code map
        val result = parseLocation("Sir George Williams Campus - Hall Building Rm 862")
        assertEquals(LocationResult.Unknown, result)
    }

    // ── ParsedLocation helpers ────────────────────────────────────────────────

    @Test
    fun `shortSummary contains all three parts`() {
        val loc = ParsedLocation("H", "Henry F. Hall", "820", CAMPUS_SGW)
        assertTrue(loc.shortSummary.contains("Henry F. Hall"))
        assertTrue(loc.shortSummary.contains("H-820"))
        assertTrue(loc.shortSummary.contains(CAMPUS_SGW))
    }
}
