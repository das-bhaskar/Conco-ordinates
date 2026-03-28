package com.example.myapplication.logic

import androidx.compose.ui.geometry.Offset
import com.example.myapplication.data.indoor.IndoorFloor
import com.example.myapplication.data.indoor.IndoorNode
import com.example.myapplication.data.indoor.IndoorRepository
import com.example.myapplication.data.indoor.IndoorRoom
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.mockito.kotlin.*

/**
 * Unit tests for [IndoorRoomResolver].
 * Uses a mock [IndoorRepository] — no Context or JSON files needed.
 */
class IndoorRoomResolverTest {

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun room(id: String, label: String) = IndoorRoom(
        id      = id,
        type    = "classroom",
        label   = label,
        polygon = listOf(Offset(0.5f, 0.5f)) // centroid at (0.5, 0.5)
    )

    private fun node(id: String, roomId: String? = null, x: Float = 0.5f, y: Float = 0.5f) =
        IndoorNode(id = id, x = x, y = y, type = "ROOM", roomId = roomId)

    private fun floor(building: String, floorNum: Int,
                      rooms: List<IndoorRoom>, nodes: List<IndoorNode>) =
        IndoorFloor(building = building, floor = floorNum, rooms = rooms, nodes = nodes)

    private suspend fun makeRepo(vararg floors: IndoorFloor): IndoorRepository {
        val repo = mock<IndoorRepository>()
        // default: return null for all floors
        whenever(repo.getFloor(any(), any())).thenReturn(null)
        for (f in floors) {
            whenever(repo.getFloor(f.building, f.floor)).thenReturn(f)
        }
        return repo
    }

    // ── Exact label match ─────────────────────────────────────────────────────

    @Test
    fun `resolve finds room by exact label`() = runTest {
        val r    = room("H-8-829", "H-829")
        val n    = node("node-829", roomId = "H-8-829")
        val repo = makeRepo(floor("H", 8, listOf(r), listOf(n)))

        val result = IndoorRoomResolver.resolve(repo, "H", "H-829", listOf(8))
        assertNotNull(result)
        assertEquals("H-8-829", result!!.roomId)
        assertEquals("node-829", result.nodeId)
        assertEquals("H-829", result.label)
        assertEquals(8, result.floor)
    }

    @Test
    fun `resolve is case insensitive`() = runTest {
        val r    = room("H-8-829", "H-829")
        val n    = node("node-829", roomId = "H-8-829")
        val repo = makeRepo(floor("H", 8, listOf(r), listOf(n)))

        val result = IndoorRoomResolver.resolve(repo, "H", "h-829", listOf(8))
        assertNotNull(result)
    }

    // ── Suffix match ──────────────────────────────────────────────────────────

    @Test
    fun `resolve finds room by room number suffix`() = runTest {
        val r    = room("H-8-829", "H-829")
        val n    = node("node-829", roomId = "H-8-829")
        val repo = makeRepo(floor("H", 8, listOf(r), listOf(n)))

        val result = IndoorRoomResolver.resolve(repo, "H", "829", listOf(8))
        assertNotNull(result)
    }

    @Test
    fun `resolve finds room by id suffix`() = runTest {
        val r    = room("H-8-829", "H-829")
        val n    = node("node-829", roomId = "H-8-829")
        val repo = makeRepo(floor("H", 8, listOf(r), listOf(n)))

        val result = IndoorRoomResolver.resolve(repo, "H", "8-829", listOf(8))
        assertNotNull(result)
    }

    // ── Multi-floor search ────────────────────────────────────────────────────

    @Test
    fun `resolve scans multiple floors and finds room on correct floor`() = runTest {
        val rFloor1 = room("H-1-110", "H-110")
        val nFloor1 = node("node-110", roomId = "H-1-110")
        val rFloor8 = room("H-8-829", "H-829")
        val nFloor8 = node("node-829", roomId = "H-8-829")

        val repo = makeRepo(
            floor("H", 1, listOf(rFloor1), listOf(nFloor1)),
            floor("H", 8, listOf(rFloor8), listOf(nFloor8))
        )

        val result = IndoorRoomResolver.resolve(repo, "H", "829", listOf(1, 8))
        assertNotNull(result)
        assertEquals(8, result!!.floor)
    }

    // ── Node fallback (no roomId linkage) ─────────────────────────────────────

    @Test
    fun `resolve falls back to nearest node by centroid when no roomId link`() = runTest {
        val r = room("H-8-829", "H-829") // polygon centroid at (0.5, 0.5)
        // Node has no roomId link — resolver should pick nearest to centroid
        val n = node("node-near", roomId = null, x = 0.5f, y = 0.5f)
        val repo = makeRepo(floor("H", 8, listOf(r), listOf(n)))

        val result = IndoorRoomResolver.resolve(repo, "H", "H-829", listOf(8))
        assertNotNull(result)
        assertEquals("node-near", result!!.nodeId)
    }

    // ── Not found cases ───────────────────────────────────────────────────────

    @Test
    fun `resolve returns null when room not found on any floor`() = runTest {
        val r    = room("H-8-829", "H-829")
        val n    = node("node-829", roomId = "H-8-829")
        val repo = makeRepo(floor("H", 8, listOf(r), listOf(n)))

        val result = IndoorRoomResolver.resolve(repo, "H", "DOESNOTEXIST", listOf(8))
        assertNull(result)
    }

    @Test
    fun `resolve returns null when floor data is null`() = runTest {
        val repo = makeRepo() // no floors available
        val result = IndoorRoomResolver.resolve(repo, "H", "829", listOf(8))
        assertNull(result)
    }

    @Test
    fun `resolve returns null when nodes list is empty`() = runTest {
        val r    = room("H-8-829", "H-829")
        val repo = makeRepo(floor("H", 8, listOf(r), emptyList()))

        val result = IndoorRoomResolver.resolve(repo, "H", "H-829", listOf(8))
        assertNull(result)
    }

    // ── ResolvedRoom data class ───────────────────────────────────────────────

    @Test
    fun `ResolvedRoom data class equality and copy`() {
        val r1 = IndoorRoomResolver.ResolvedRoom("H", 8, "H-8-829", "node-829", "H-829")
        val r2 = r1.copy()
        assertEquals(r1, r2)
        assertNotEquals(r1, r1.copy(floor = 1))
        assertNotNull(r1.toString())
        assertEquals(r1.hashCode(), r2.hashCode())
    }

    // ── resolveEntrance ───────────────────────────────────────────────────────

    @Test
    fun `resolveEntrance returns null (stub - not yet implemented)`() = runTest {
        val repo = makeRepo()
        val result = IndoorRoomResolver.resolveEntrance(repo, "H", 1)
        assertNull(result)
    }
}
