package com.example.myapplication.data.indoor

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for [IndoorBuildingConfig].
 * Pure JVM — no Android dependencies.
 */
class IndoorBuildingConfigTest {

    // ── floorsFor ─────────────────────────────────────────────────────────────

    @Test
    fun `floorsFor returns correct floors for H building`() {
        assertEquals(listOf(1, 2, 8, 9), IndoorBuildingConfig.floorsFor("H"))
    }

    @Test
    fun `floorsFor returns correct floors for CC building`() {
        assertEquals(listOf(1), IndoorBuildingConfig.floorsFor("CC"))
    }

    @Test
    fun `floorsFor returns correct floors for MB building including basement`() {
        val floors = IndoorBuildingConfig.floorsFor("MB")
        assertTrue(floors.contains(1))
        assertTrue(floors.contains(-2))
    }

    @Test
    fun `floorsFor returns correct floors for VE building`() {
        assertEquals(listOf(1, 2), IndoorBuildingConfig.floorsFor("VE"))
    }

    @Test
    fun `floorsFor returns correct floors for VL building`() {
        assertEquals(listOf(1, 2), IndoorBuildingConfig.floorsFor("VL"))
    }

    @Test
    fun `floorsFor is case insensitive`() {
        assertEquals(IndoorBuildingConfig.floorsFor("H"), IndoorBuildingConfig.floorsFor("h"))
        assertEquals(IndoorBuildingConfig.floorsFor("CC"), IndoorBuildingConfig.floorsFor("cc"))
    }

    @Test
    fun `floorsFor returns empty list for unknown building`() {
        assertTrue(IndoorBuildingConfig.floorsFor("UNKNOWN").isEmpty())
        assertTrue(IndoorBuildingConfig.floorsFor("").isEmpty())
        assertTrue(IndoorBuildingConfig.floorsFor("XYZ").isEmpty())
    }

    // ── hasIndoorMap ──────────────────────────────────────────────────────────

    @Test
    fun `hasIndoorMap returns true for known buildings`() {
        assertTrue(IndoorBuildingConfig.hasIndoorMap("H"))
        assertTrue(IndoorBuildingConfig.hasIndoorMap("CC"))
        assertTrue(IndoorBuildingConfig.hasIndoorMap("MB"))
        assertTrue(IndoorBuildingConfig.hasIndoorMap("VE"))
        assertTrue(IndoorBuildingConfig.hasIndoorMap("VL"))
    }

    @Test
    fun `hasIndoorMap returns false for unknown building`() {
        assertFalse(IndoorBuildingConfig.hasIndoorMap("UNKNOWN"))
        assertFalse(IndoorBuildingConfig.hasIndoorMap(""))
        assertFalse(IndoorBuildingConfig.hasIndoorMap("EV"))
    }

    @Test
    fun `hasIndoorMap is case insensitive`() {
        assertTrue(IndoorBuildingConfig.hasIndoorMap("h"))
        assertTrue(IndoorBuildingConfig.hasIndoorMap("cc"))
        assertTrue(IndoorBuildingConfig.hasIndoorMap("Mb"))
    }

    // ── allCodes ──────────────────────────────────────────────────────────────

    @Test
    fun `allCodes contains all known building codes`() {
        val codes = IndoorBuildingConfig.allCodes
        assertTrue(codes.contains("H"))
        assertTrue(codes.contains("CC"))
        assertTrue(codes.contains("MB"))
        assertTrue(codes.contains("VE"))
        assertTrue(codes.contains("VL"))
    }

    @Test
    fun `allCodes size matches number of supported buildings`() {
        assertEquals(5, IndoorBuildingConfig.allCodes.size)
    }
}
