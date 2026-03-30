package com.example.myapplication.data.indoor

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for all data classes in IndoorModels.kt.
 * Covers: equality, copy, toString, hashCode, default values.
 */
class IndoorModelsTest {

    // ── IndoorRoom ─────────────────────────────────────────────────────────────

    @Test fun `IndoorRoom equality and copy`() {
        val r1 = IndoorRoom("id1", "classroom", "Room 101",
            icon = "📚", polygon = listOf(Offset(0f, 0f)), accessible = true)
        val r2 = r1.copy()
        assertEquals(r1, r2)
        assertEquals(r1.hashCode(), r2.hashCode())
        assertNotEquals(r1, r1.copy(id = "other"))
    }

    @Test fun `IndoorRoom default values`() {
        val r = IndoorRoom("id", "other", "Label", polygon = emptyList())
        assertNull(r.icon)
        assertTrue(r.accessible)
    }

    @Test fun `IndoorRoom toString contains id`() {
        val r = IndoorRoom("myId", "classroom", "Label", polygon = emptyList())
        assertTrue(r.toString().contains("myId"))
    }

    // ── IndoorCorridor ─────────────────────────────────────────────────────────

    @Test fun `IndoorCorridor equality and copy`() {
        val c1 = IndoorCorridor("c1", listOf(Offset(0.1f, 0.2f)))
        val c2 = c1.copy()
        assertEquals(c1, c2)
        assertNotEquals(c1, c1.copy(id = "c2"))
    }

    @Test fun `IndoorCorridor empty polygon`() {
        val c = IndoorCorridor("c", emptyList())
        assertTrue(c.polygon.isEmpty())
    }

    // ── IndoorNode ─────────────────────────────────────────────────────────────

    @Test fun `IndoorNode equality and copy`() {
        val n1 = IndoorNode("n1", 0.5f, 0.5f, "ROOM", roomId = "r1", elevatorGroupId = null)
        val n2 = n1.copy()
        assertEquals(n1, n2)
        assertEquals(n1.hashCode(), n2.hashCode())
        assertNotEquals(n1, n1.copy(id = "n2"))
    }

    @Test fun `IndoorNode default values`() {
        val n = IndoorNode("n", 0f, 0f, "CORRIDOR")
        assertNull(n.roomId)
        assertNull(n.elevatorGroupId)
        assertTrue(n.accessible)
    }

    @Test fun `IndoorNode with elevatorGroupId`() {
        val n = IndoorNode("n", 0f, 0f, "ELEVATOR", elevatorGroupId = "elev-A")
        assertEquals("elev-A", n.elevatorGroupId)
    }

    // ── IndoorEdge ─────────────────────────────────────────────────────────────

    @Test fun `IndoorEdge equality and copy`() {
        val e1 = IndoorEdge("a", "b", 2.5f, "SMOOTH", true)
        val e2 = e1.copy()
        assertEquals(e1, e2)
        assertEquals(e1.hashCode(), e2.hashCode())
        assertNotEquals(e1, e1.copy(from = "x"))
    }

    @Test fun `IndoorEdge default values`() {
        val e = IndoorEdge("a", "b")
        assertEquals(1f, e.weight)
        assertEquals("SMOOTH", e.surface)
        assertTrue(e.accessible)
    }

    // ── IndoorPoi ─────────────────────────────────────────────────────────────

    @Test fun `IndoorPoi equality and copy`() {
        val p1 = IndoorPoi("p1", "washroom", "WC", 0.3f, 0.7f, "node-wc")
        val p2 = p1.copy()
        assertEquals(p1, p2)
        assertNotEquals(p1, p1.copy(id = "p2"))
    }

    @Test fun `IndoorPoi default nodeId`() {
        val p = IndoorPoi("p", "other", "Label", 0f, 0f)
        assertEquals("", p.nodeId)
    }

    // ── IndoorEntrance ────────────────────────────────────────────────────────

    @Test fun `IndoorEntrance equality and copy`() {
        val e1 = IndoorEntrance("e1", "Main Entrance", 0.1f, 0.9f, "node-e1", floor = 1)
        val e2 = e1.copy()
        assertEquals(e1, e2)
        assertNotEquals(e1, e1.copy(id = "e2"))
    }

    @Test fun `IndoorEntrance default values`() {
        val e = IndoorEntrance("e", x = 0f, y = 0f)
        assertEquals("", e.label)
        assertEquals("", e.nodeId)
        assertEquals(1, e.floor)
    }

    // ── IndoorFloor ───────────────────────────────────────────────────────────

    @Test fun `IndoorFloor equality and copy`() {
        val f1 = IndoorFloor("H", 8)
        val f2 = f1.copy()
        assertEquals(f1, f2)
        assertNotEquals(f1, f1.copy(floor = 1))
    }

    @Test fun `IndoorFloor default collections are empty`() {
        val f = IndoorFloor("CC", 1)
        assertTrue(f.rooms.isEmpty())
        assertTrue(f.corridors.isEmpty())
        assertTrue(f.nodes.isEmpty())
        assertTrue(f.edges.isEmpty())
        assertTrue(f.pois.isEmpty())
        assertTrue(f.entrances.isEmpty())
    }

    @Test fun `IndoorFloor negative floor number (basement)`() {
        val f = IndoorFloor("MB", -2)
        assertEquals(-2, f.floor)
    }

    @Test fun `IndoorFloor hashCode consistent`() {
        val f = IndoorFloor("H", 8)
        assertEquals(f.hashCode(), f.copy().hashCode())
    }

    @Test fun `IndoorFloor toString contains building and floor`() {
        val f = IndoorFloor("H", 8)
        assertTrue(f.toString().contains("H"))
        assertTrue(f.toString().contains("8"))
    }
}
