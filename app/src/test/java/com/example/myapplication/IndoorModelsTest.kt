package com.example.myapplication.data.indoor

import androidx.compose.ui.geometry.Offset
import com.example.myapplication.logic.TransferPreference
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for indoor data models and [TransferPreference].
 * Pure JVM — exercises data class generated methods and enum properties.
 */
class IndoorModelsTest {

    // ── IndoorRoom ────────────────────────────────────────────────────────────

    @Test
    fun `IndoorRoom data class equality and copy`() {
        val room = IndoorRoom(
            id        = "H-8-829",
            type      = "classroom",
            label     = "H-829",
            polygon   = listOf(Offset(0f, 0f), Offset(1f, 1f)),
            accessible = true
        )
        assertEquals(room, room.copy())
        assertNotEquals(room, room.copy(id = "H-8-830"))
        assertNotNull(room.toString())
        assertEquals(room.hashCode(), room.hashCode())
    }

    @Test
    fun `IndoorRoom icon defaults to null`() {
        val room = IndoorRoom(id = "R", type = "other", label = "R", polygon = emptyList())
        assertNull(room.icon)
    }

    // ── IndoorNode ────────────────────────────────────────────────────────────

    @Test
    fun `IndoorNode data class equality and copy`() {
        val node = IndoorNode(id = "node-1", x = 0.5f, y = 0.5f, type = "CORRIDOR")
        assertEquals(node, node.copy())
        assertNotEquals(node, node.copy(id = "node-2"))
        assertNotNull(node.toString())
    }

    @Test
    fun `IndoorNode elevatorGroupId defaults to null`() {
        val node = IndoorNode(id = "n", x = 0f, y = 0f, type = "ELEVATOR")
        assertNull(node.elevatorGroupId)
    }

    // ── IndoorEdge ────────────────────────────────────────────────────────────

    @Test
    fun `IndoorEdge defaults weight to 1 and accessible to true`() {
        val edge = IndoorEdge(from = "A", to = "B")
        assertEquals(1f, edge.weight, 0.001f)
        assertTrue(edge.accessible)
    }

    @Test
    fun `IndoorEdge data class equality`() {
        val edge = IndoorEdge(from = "A", to = "B", weight = 2f)
        assertEquals(edge, edge.copy())
        assertNotEquals(edge, edge.copy(weight = 3f))
    }

    // ── IndoorFloor ───────────────────────────────────────────────────────────

    @Test
    fun `IndoorFloor defaults to empty lists`() {
        val floor = IndoorFloor(building = "H", floor = 8)
        assertTrue(floor.rooms.isEmpty())
        assertTrue(floor.nodes.isEmpty())
        assertTrue(floor.edges.isEmpty())
        assertTrue(floor.corridors.isEmpty())
        assertTrue(floor.pois.isEmpty())
        assertTrue(floor.entrances.isEmpty())
    }

    @Test
    fun `IndoorFloor data class equality`() {
        val floor = IndoorFloor(building = "H", floor = 8)
        assertEquals(floor, floor.copy())
        assertNotEquals(floor, floor.copy(floor = 9))
    }

    // ── IndoorPoi ─────────────────────────────────────────────────────────────

    @Test
    fun `IndoorPoi data class coverage`() {
        val poi = IndoorPoi(id = "p1", type = "washroom", label = "WC", x = 0.5f, y = 0.5f)
        assertEquals("p1", poi.id)
        assertEquals(poi, poi.copy())
        assertNotNull(poi.toString())
    }

    // ── IndoorEntrance ────────────────────────────────────────────────────────

    @Test
    fun `IndoorEntrance defaults floor to 1`() {
        val entrance = IndoorEntrance(id = "ent-1", x = 0f, y = 0f)
        assertEquals(1, entrance.floor)
    }

    @Test
    fun `IndoorEntrance data class equality`() {
        val ent = IndoorEntrance(id = "e", label = "East", x = 0.1f, y = 0.9f, nodeId = "n1")
        assertEquals(ent, ent.copy())
        assertNotEquals(ent, ent.copy(label = "West"))
    }

    // ── IndoorCorridor ────────────────────────────────────────────────────────

    @Test
    fun `IndoorCorridor data class coverage`() {
        val corridor = IndoorCorridor(
            id      = "corr-1",
            polygon = listOf(Offset(0f, 0f), Offset(1f, 0f))
        )
        assertEquals("corr-1", corridor.id)
        assertEquals(corridor, corridor.copy())
    }

    // ── TransferPreference ────────────────────────────────────────────────────

    @Test
    fun `TransferPreference ANY has all types as primary`() {
        val pref = TransferPreference.ANY
        assertTrue(pref.primary.contains("ELEVATOR"))
        assertTrue(pref.primary.contains("ESCALATOR"))
        assertTrue(pref.primary.contains("STAIRCASE"))
        assertTrue(pref.fallback.isEmpty())
    }

    @Test
    fun `TransferPreference ELEVATOR_ONLY only uses elevator`() {
        val pref = TransferPreference.ELEVATOR_ONLY
        assertEquals(listOf("ELEVATOR"), pref.primary)
        assertTrue(pref.fallback.isEmpty())
    }

    @Test
    fun `TransferPreference ESCALATOR falls back to elevator`() {
        val pref = TransferPreference.ESCALATOR
        assertTrue(pref.primary.contains("ESCALATOR"))
        assertTrue(pref.fallback.contains("ELEVATOR"))
    }

    @Test
    fun `TransferPreference STAIRS falls back to elevator`() {
        val pref = TransferPreference.STAIRS
        assertTrue(pref.primary.contains("STAIRCASE"))
        assertTrue(pref.fallback.contains("ELEVATOR"))
    }

    @Test
    fun `TransferPreference all entries have non-blank label and icon`() {
        TransferPreference.entries.forEach { pref ->
            assertTrue(pref.label.isNotBlank())
            assertTrue(pref.icon.isNotBlank())
        }
    }

    @Test
    fun `TransferPreference valueOf works for all entries`() {
        assertEquals(TransferPreference.ANY,            TransferPreference.valueOf("ANY"))
        assertEquals(TransferPreference.ELEVATOR_ONLY,  TransferPreference.valueOf("ELEVATOR_ONLY"))
        assertEquals(TransferPreference.ESCALATOR,      TransferPreference.valueOf("ESCALATOR"))
        assertEquals(TransferPreference.STAIRS,         TransferPreference.valueOf("STAIRS"))
    }
}
