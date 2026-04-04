package com.example.myapplication.logic

import androidx.compose.ui.geometry.Offset
import com.example.myapplication.data.indoor.IndoorEdge
import com.example.myapplication.data.indoor.IndoorFloor
import com.example.myapplication.data.indoor.IndoorNode
import com.example.myapplication.data.indoor.IndoorRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.mockito.kotlin.*

/**
 * Unit tests for [CrossFloorNavigator].
 * Uses a mock [IndoorRepository] — no Context or JSON needed.
 *
 * Floor 1 layout:  start ──── elevator_F1
 * Floor 8 layout:  elevator_F8 ──── dest
 *
 * Both elevator nodes share elevatorGroupId = "EL-A".
 */
class CrossFloorNavigatorTest {

    // ── Test floor data ───────────────────────────────────────────────────────

    private fun node(id: String, x: Float, y: Float, type: String = "CORRIDOR",
                     groupId: String? = null) =
        IndoorNode(id = id, x = x, y = y, type = type, elevatorGroupId = groupId)

    private fun escalatorNode(
        id: String,
        x: Float,
        y: Float,
        targetFloor: Int? = null,
        targetNodeId: String? = null
    ) = IndoorNode(
        id = id,
        x = x,
        y = y,
        type = "ESCALATOR",
        transferFloor = targetFloor,
        transferNodeId = targetNodeId
    )

    private fun edge(from: String, to: String) = IndoorEdge(from = from, to = to)

    private val startNode   = node("start",       0f, 0f)
    private val elF1        = node("el_f1",       1f, 0f, "ELEVATOR", "EL-A")
    private val elF8        = node("el_f8",       0f, 0f, "ELEVATOR", "EL-A")
    private val destNode    = node("dest",        1f, 0f)

    private val floor1 = IndoorFloor(
        building = "H",
        floor    = 1,
        nodes    = listOf(startNode, elF1),
        edges    = listOf(edge("start", "el_f1"))
    )

    private val floor8 = IndoorFloor(
        building = "H",
        floor    = 8,
        nodes    = listOf(elF8, destNode),
        edges    = listOf(edge("el_f8", "dest"))
    )

    private fun makeRepo(f1: IndoorFloor? = floor1, f8: IndoorFloor? = floor8): IndoorRepository {
        val repo = mock<IndoorRepository>()
        runBlocking { whenever(repo.getFloor("H", 1)).thenReturn(f1) }
        runBlocking { whenever(repo.getFloor("H", 8)).thenReturn(f8) }
        return repo
    }

    @Suppress("UNUSED")
    private fun runBlocking(block: suspend () -> Unit) =
        kotlinx.coroutines.runBlocking { block() }

    // ── Same floor ────────────────────────────────────────────────────────────

    @Test
    fun `navigate same floor returns single Walk step`() = runTest {
        val repo = makeRepo()
        val steps = CrossFloorNavigator.navigate(
            repo         = repo,
            building     = "H",
            startFloor   = 1,
            startNodeId  = "start",
            targetFloor  = 1,
            targetNodeId = "el_f1"
        )
        assertEquals(1, steps.size)
        assertTrue(steps[0] is CrossFloorNavigator.NavStep.Walk)
    }

    @Test
    fun `navigate same floor with same start and end returns single node path`() = runTest {
        val repo = makeRepo()
        val steps = CrossFloorNavigator.navigate(
            repo         = repo,
            building     = "H",
            startFloor   = 1,
            startNodeId  = "start",
            targetFloor  = 1,
            targetNodeId = "start"
        )
        assertEquals(1, steps.size)
        val walk = steps[0] as CrossFloorNavigator.NavStep.Walk
        assertEquals(1, walk.segment.path.size)
    }

    // ── Cross floor ───────────────────────────────────────────────────────────

    @Test
    fun `navigate cross floor returns Walk ChangeFloor Walk`() = runTest {
        val repo = makeRepo()
        val steps = CrossFloorNavigator.navigate(
            repo         = repo,
            building     = "H",
            startFloor   = 1,
            startNodeId  = "start",
            targetFloor  = 8,
            targetNodeId = "dest"
        )
        assertEquals(3, steps.size)
        assertTrue(steps[0] is CrossFloorNavigator.NavStep.Walk)
        assertTrue(steps[1] is CrossFloorNavigator.NavStep.ChangeFloor)
        assertTrue(steps[2] is CrossFloorNavigator.NavStep.Walk)
    }

    @Test
    fun `navigate cross floor ChangeFloor has correct floors`() = runTest {
        val repo = makeRepo()
        val steps = CrossFloorNavigator.navigate(
            repo         = repo,
            building     = "H",
            startFloor   = 1,
            startNodeId  = "start",
            targetFloor  = 8,
            targetNodeId = "dest"
        )
        val change = steps[1] as CrossFloorNavigator.NavStep.ChangeFloor
        assertEquals(1, change.fromFloor)
        assertEquals(8, change.toFloor)
        assertEquals(CrossFloorNavigator.VIA_ELEVATOR, change.via)
    }

    // ── No path cases ─────────────────────────────────────────────────────────

    @Test
    fun `navigate returns empty when start floor data is null`() = runTest {
        val repo = makeRepo(f1 = null)
        val steps = CrossFloorNavigator.navigate(
            repo         = repo,
            building     = "H",
            startFloor   = 1,
            startNodeId  = "start",
            targetFloor  = 8,
            targetNodeId = "dest"
        )
        assertTrue(steps.isEmpty())
    }

    @Test
    fun `navigate returns empty when target floor data is null`() = runTest {
        val repo = makeRepo(f8 = null)
        val steps = CrossFloorNavigator.navigate(
            repo         = repo,
            building     = "H",
            startFloor   = 1,
            startNodeId  = "start",
            targetFloor  = 8,
            targetNodeId = "dest"
        )
        assertTrue(steps.isEmpty())
    }

    @Test
    fun `navigate returns empty when no transfer nodes exist`() = runTest {
        // Floor 1 has no elevator node
        val f1NoElevator = IndoorFloor(
            building = "H", floor = 1,
            nodes    = listOf(startNode),
            edges    = emptyList()
        )
        val repo = makeRepo(f1 = f1NoElevator)
        val steps = CrossFloorNavigator.navigate(
            repo         = repo,
            building     = "H",
            startFloor   = 1,
            startNodeId  = "start",
            targetFloor  = 8,
            targetNodeId = "dest"
        )
        assertTrue(steps.isEmpty())
    }

    // ── TransferPreference ────────────────────────────────────────────────────

    @Test
    fun `navigate with ELEVATOR_ONLY uses elevator`() = runTest {
        val repo = makeRepo()
        val steps = CrossFloorNavigator.navigate(
            repo         = repo,
            building     = "H",
            startFloor   = 1,
            startNodeId  = "start",
            targetFloor  = 8,
            targetNodeId = "dest",
            preference   = TransferPreference.ELEVATOR_ONLY
        )
        assertTrue(steps.isNotEmpty())
        val change = steps.filterIsInstance<CrossFloorNavigator.NavStep.ChangeFloor>().first()
        assertEquals(CrossFloorNavigator.VIA_ELEVATOR, change.via)
    }

    @Test
    fun `navigate can chain directed escalators across multiple floors`() = runTest {
        val floor1Esc = IndoorFloor(
            building = "H",
            floor = 1,
            nodes = listOf(
                node("start", 0f, 0f),
                escalatorNode("es_1_2", 1f, 0f, targetFloor = 2, targetNodeId = "es_2_from_1")
            ),
            edges = listOf(edge("start", "es_1_2"))
        )
        val floor2Esc = IndoorFloor(
            building = "H",
            floor = 2,
            nodes = listOf(
                escalatorNode("es_2_from_1", 0f, 0f),
                escalatorNode("es_2_8", 1f, 0f, targetFloor = 8, targetNodeId = "es_8_from_2")
            ),
            edges = listOf(edge("es_2_from_1", "es_2_8"))
        )
        val floor8Esc = IndoorFloor(
            building = "H",
            floor = 8,
            nodes = listOf(
                escalatorNode("es_8_from_2", 0f, 0f),
                escalatorNode("es_8_9", 1f, 0f, targetFloor = 9, targetNodeId = "es_9_from_8")
            ),
            edges = listOf(edge("es_8_from_2", "es_8_9"))
        )
        val floor9Esc = IndoorFloor(
            building = "H",
            floor = 9,
            nodes = listOf(
                escalatorNode("es_9_from_8", 0f, 0f),
                node("dest", 1f, 0f)
            ),
            edges = listOf(edge("es_9_from_8", "dest"))
        )

        val repo = mock<IndoorRepository>()
        runBlocking { whenever(repo.getFloor("H", 1)).thenReturn(floor1Esc) }
        runBlocking { whenever(repo.getFloor("H", 2)).thenReturn(floor2Esc) }
        runBlocking { whenever(repo.getFloor("H", 8)).thenReturn(floor8Esc) }
        runBlocking { whenever(repo.getFloor("H", 9)).thenReturn(floor9Esc) }

        val steps = CrossFloorNavigator.navigate(
            repo = repo,
            building = "H",
            startFloor = 1,
            startNodeId = "start",
            targetFloor = 9,
            targetNodeId = "dest",
            preference = TransferPreference.ESCALATOR
        )

        assertEquals(7, steps.size)
        assertEquals(
            listOf(2, 8, 9),
            steps.filterIsInstance<CrossFloorNavigator.NavStep.ChangeFloor>().map { it.toFloor }
        )
        assertTrue(
            steps.filterIsInstance<CrossFloorNavigator.NavStep.ChangeFloor>()
                .all { it.via == CrossFloorNavigator.VIA_ESCALATOR }
        )
    }

    // ── NavStep data classes ──────────────────────────────────────────────────

    @Test
    fun `FloorSegment data class equality`() {
        val seg1 = CrossFloorNavigator.FloorSegment(1, listOf(startNode), "H")
        val seg2 = CrossFloorNavigator.FloorSegment(1, listOf(startNode), "H")
        assertEquals(seg1, seg2)
    }

    @Test
    fun `NavStep ChangeFloor data class equality`() {
        val c1 = CrossFloorNavigator.NavStep.ChangeFloor(1, 8, "elevator", "H", "el_f8")
        val c2 = CrossFloorNavigator.NavStep.ChangeFloor(1, 8, "elevator", "H", "el_f8")
        assertEquals(c1, c2)
        assertNotEquals(c1, c1.copy(toFloor = 9))
    }
}
