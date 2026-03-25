package com.example.myapplication

import com.example.myapplication.ui.models.NavigationState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationStateTest {

    @Test
    fun `default state has expected values`() {
        val state = NavigationState()

        assertFalse(state.isAutoCenterEnabled)
        assertFalse(state.hasArrived)
        assertEquals("Follow the path", state.currentInstruction)
        assertEquals(0f, state.currentBearing)
    }

    @Test
    fun `copy updates only selected fields`() {
        val initial = NavigationState()

        val updated = initial.copy(
            isAutoCenterEnabled = true,
            hasArrived = true,
            currentInstruction = "Turn left",
            currentBearing = 90f
        )

        assertTrue(updated.isAutoCenterEnabled)
        assertTrue(updated.hasArrived)
        assertEquals("Turn left", updated.currentInstruction)
        assertEquals(90f, updated.currentBearing)
    }
}
