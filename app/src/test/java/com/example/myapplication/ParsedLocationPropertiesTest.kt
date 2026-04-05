package com.example.myapplication

import com.example.myapplication.data.LocationResult
import com.example.myapplication.data.ParsedLocation
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for [ParsedLocation] computed properties and
 * [LocationResult] sealed-class identity / equality.
 */
class ParsedLocationPropertiesTest {

    // ── ParsedLocation computed properties ────────────────────────────────────

    @Test
    fun `roomDisplay formats as code-room`() {
        val loc = ParsedLocation("H", "Henry F. Hall Building", "820", "Sir George Williams")
        assertEquals("H-820", loc.roomDisplay)
    }

    @Test
    fun `shortSummary includes name, room display, and campus`() {
        val loc = ParsedLocation("MB", "John Molson Building", "S1.401", "Sir George Williams")
        assertEquals("John Molson Building · MB-S1.401 · Sir George Williams", loc.shortSummary)
    }

    @Test
    fun `roomDisplay handles empty room code`() {
        val loc = ParsedLocation("H", "Hall Building", "", "SGW")
        assertEquals("H-", loc.roomDisplay)
    }

    @Test
    fun `data class equality works`() {
        val a = ParsedLocation("H", "Hall", "820", "SGW")
        val b = ParsedLocation("H", "Hall", "820", "SGW")
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `data class inequality on different fields`() {
        val a = ParsedLocation("H", "Hall", "820", "SGW")
        val b = ParsedLocation("MB", "Molson", "100", "SGW")
        assertNotEquals(a, b)
    }

    @Test
    fun `copy changes only specified fields`() {
        val original = ParsedLocation("H", "Hall", "820", "SGW")
        val copied = original.copy(roomCode = "821")
        assertEquals("H", copied.buildingCode)
        assertEquals("821", copied.roomCode)
    }

    // ── LocationResult sealed class ───────────────────────────────────────────

    @Test
    fun `Known instances with same ParsedLocation are equal`() {
        val loc = ParsedLocation("H", "Hall", "820", "SGW")
        assertEquals(LocationResult.Known(loc), LocationResult.Known(loc))
    }

    @Test
    fun `Online is a singleton`() {
        assertSame(LocationResult.Online, LocationResult.Online)
    }

    @Test
    fun `TBA is a singleton`() {
        assertSame(LocationResult.TBA, LocationResult.TBA)
    }

    @Test
    fun `Unknown is a singleton`() {
        assertSame(LocationResult.Unknown, LocationResult.Unknown)
    }

    @Test
    fun `different LocationResult subtypes are not equal`() {
        assertNotEquals(LocationResult.Online as LocationResult, LocationResult.TBA as LocationResult)
        assertNotEquals(LocationResult.Unknown as LocationResult, LocationResult.Online as LocationResult)
    }

    @Test
    fun `Known location property is accessible`() {
        val loc = ParsedLocation("EV", "EV Complex", "3.309", "SGW")
        val result = LocationResult.Known(loc)
        assertEquals(loc, result.location)
    }
}
