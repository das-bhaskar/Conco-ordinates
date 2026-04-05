package com.example.myapplication

import com.example.myapplication.logic.TransferPreference
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for [TransferPreference] enum properties and lookup behaviour.
 */
class TransferPreferenceTest {

    // ── Any ───────────────────────────────────────────────────────────────────

    @Test
    fun `ANY label is Any (Shortest)`() {
        assertEquals("Any (Shortest)", TransferPreference.ANY.label)
    }

    @Test
    fun `ANY icon is shuffle emoji`() {
        assertEquals("🔀", TransferPreference.ANY.icon)
    }

    @Test
    fun `ANY primary includes all three transfer types`() {
        assertEquals(
            listOf("ELEVATOR", "ESCALATOR", "STAIRCASE"),
            TransferPreference.ANY.primary
        )
    }

    @Test
    fun `ANY fallback is empty`() {
        assertTrue(TransferPreference.ANY.fallback.isEmpty())
    }

    // ── Elevator Only ─────────────────────────────────────────────────────────

    @Test
    fun `ELEVATOR_ONLY primary is elevator only`() {
        assertEquals(listOf("ELEVATOR"), TransferPreference.ELEVATOR_ONLY.primary)
    }

    @Test
    fun `ELEVATOR_ONLY fallback is empty`() {
        assertTrue(TransferPreference.ELEVATOR_ONLY.fallback.isEmpty())
    }

    @Test
    fun `ELEVATOR_ONLY label is Elevator`() {
        assertEquals("Elevator", TransferPreference.ELEVATOR_ONLY.label)
    }

    // ── Escalator ─────────────────────────────────────────────────────────────

    @Test
    fun `ESCALATOR primary is escalator`() {
        assertEquals(listOf("ESCALATOR"), TransferPreference.ESCALATOR.primary)
    }

    @Test
    fun `ESCALATOR fallback is elevator`() {
        assertEquals(listOf("ELEVATOR"), TransferPreference.ESCALATOR.fallback)
    }

    @Test
    fun `ESCALATOR label and icon`() {
        assertEquals("Escalator", TransferPreference.ESCALATOR.label)
        assertEquals("↗", TransferPreference.ESCALATOR.icon)
    }

    // ── Stairs ────────────────────────────────────────────────────────────────

    @Test
    fun `STAIRS primary is staircase`() {
        assertEquals(listOf("STAIRCASE"), TransferPreference.STAIRS.primary)
    }

    @Test
    fun `STAIRS fallback is elevator`() {
        assertEquals(listOf("ELEVATOR"), TransferPreference.STAIRS.fallback)
    }

    @Test
    fun `STAIRS label and icon`() {
        assertEquals("Stairs", TransferPreference.STAIRS.label)
        assertEquals("🪜", TransferPreference.STAIRS.icon)
    }

    // ── Enum completeness ─────────────────────────────────────────────────────

    @Test
    fun `all four preferences are defined`() {
        val values = TransferPreference.entries
        assertEquals(4, values.size)
        assertTrue(values.contains(TransferPreference.ANY))
        assertTrue(values.contains(TransferPreference.ELEVATOR_ONLY))
        assertTrue(values.contains(TransferPreference.ESCALATOR))
        assertTrue(values.contains(TransferPreference.STAIRS))
    }

    @Test
    fun `valueOf resolves by name`() {
        assertEquals(TransferPreference.ANY, TransferPreference.valueOf("ANY"))
        assertEquals(TransferPreference.ELEVATOR_ONLY, TransferPreference.valueOf("ELEVATOR_ONLY"))
        assertEquals(TransferPreference.ESCALATOR, TransferPreference.valueOf("ESCALATOR"))
        assertEquals(TransferPreference.STAIRS, TransferPreference.valueOf("STAIRS"))
    }
}
