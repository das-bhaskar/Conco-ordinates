package com.example.myapplication

import com.example.myapplication.logic.CampusNavigationEngine
import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Additional edge-case tests for [CampusNavigationEngine].
 */
class NavigationEngineAdditionalTest {

    private lateinit var engine: CampusNavigationEngine

    @Before
    fun setup() {
        engine = CampusNavigationEngine()
    }

    // ── checkArrival edge cases ───────────────────────────────────────────────

    @Test
    fun `checkArrival returns true when user is exactly at destination`() {
        val point = LatLng(45.4972, -73.5789)
        assertTrue(engine.checkArrival(point, point))
    }

    @Test
    fun `checkArrival returns false when user is just over 15 metres away`() {
        // ~20m north
        val dest = LatLng(45.4972, -73.5789)
        val user = LatLng(45.49738, -73.5789)
        assertFalse(engine.checkArrival(user, dest))
    }

    // ── checkArrivalWithBuilding edge cases ───────────────────────────────────

    @Test
    fun `checkArrivalWithBuilding with custom small radius rejects nearby point`() {
        val center = LatLng(45.4972, -73.5789)
        // ~3m away
        val user = LatLng(45.49722, -73.5789)
        assertTrue(engine.checkArrivalWithBuilding(user, center, radiusMetres = 50.0))
        assertFalse(engine.checkArrivalWithBuilding(user, center, radiusMetres = 0.1))
    }

    @Test
    fun `checkArrivalWithBuilding with very large radius accepts far point`() {
        val center = LatLng(45.4972, -73.5789)
        val farUser = LatLng(45.498, -73.579) // ~100m away
        assertTrue(engine.checkArrivalWithBuilding(farUser, center, radiusMetres = 200.0))
    }

    // ── calculateBearing edge cases ──────────────────────────────────────────

    @Test
    fun `calculateBearing skips close points and targets far point`() {
        val user = LatLng(45.4972, -73.5789)
        // First point is <12m away, second is ~1km north
        val route = listOf(
            LatLng(45.49725, -73.5789),  // very close (~5m)
            LatLng(45.507, -73.5789)     // ~1km north
        )
        val bearing = engine.calculateBearing(user, route, 90f)
        // Should target the far point, not return currentBearing
        assertNotEquals(90f, bearing, 1f)
    }

    @Test
    fun `calculateBearing with all points close returns currentBearing`() {
        val user = LatLng(45.4972, -73.5789)
        val route = listOf(
            LatLng(45.49721, -73.5789),
            LatLng(45.49722, -73.5789)
        )
        val bearing = engine.calculateBearing(user, route, 180f)
        assertEquals(180f, bearing, 0.01f)
    }

    @Test
    fun `calculateBearing with single far point returns heading`() {
        val user = LatLng(45.4972, -73.5789)
        val route = listOf(LatLng(45.507, -73.579)) // ~1km north
        val bearing = engine.calculateBearing(user, route, 0f)
        // Heading to a point due north should be close to 0 (north)
        assertTrue(bearing in -10f..10f)
    }

    // ── calculateNextInstruction ──────────────────────────────────────────────

    @Test
    fun `calculateNextInstruction returns expected text`() {
        val result = engine.calculateNextInstruction(
            LatLng(45.497, -73.579),
            emptyList()
        )
        assertEquals("Proceed toward destination", result)
    }
}
