package com.example.myapplication.logic

import com.example.myapplication.data.indoor.IndoorEdge
import com.example.myapplication.data.indoor.IndoorNode
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for [IndoorPathfinder].
 * Pure JVM — no Android dependencies.
 *
 * Graph used in most tests (normalized coords):
 *
 *   A(0,0) ── B(0.5,0) ── C(1,0)
 *                |
 *               D(0.5,1)
 */
class IndoorPathfinderTest {

    // ── Shared test graph ─────────────────────────────────────────────────────

    private fun node(id: String, x: Float, y: Float, type: String = "CORRIDOR") =
        IndoorNode(id = id, x = x, y = y, type = type)

    private fun edge(from: String, to: String, weight: Float = 1f, accessible: Boolean = true) =
        IndoorEdge(from = from, to = to, weight = weight, accessible = accessible)

    private val nodeA = node("A", 0f, 0f)
    private val nodeB = node("B", 0.5f, 0f)
    private val nodeC = node("C", 1f, 0f)
    private val nodeD = node("D", 0.5f, 1f)

    private val nodes = listOf(nodeA, nodeB, nodeC, nodeD)
    private val edges = listOf(
        edge("A", "B"),
        edge("B", "C"),
        edge("B", "D")
    )

    // ── Basic pathfinding ─────────────────────────────────────────────────────

    @Test
    fun `findPath returns single node when start equals end`() {
        val result = IndoorPathfinder.findPath(nodes, edges, "A", "A")
        assertEquals(1, result.size)
        assertEquals("A", result[0].id)
    }

    @Test
    fun `findPath finds direct path A to C`() {
        val result = IndoorPathfinder.findPath(nodes, edges, "A", "C")
        assertEquals(listOf("A", "B", "C"), result.map { it.id })
    }

    @Test
    fun `findPath finds path A to D through B`() {
        val result = IndoorPathfinder.findPath(nodes, edges, "A", "D")
        assertEquals(listOf("A", "B", "D"), result.map { it.id })
    }

    @Test
    fun `findPath is bidirectional - C to A works`() {
        val result = IndoorPathfinder.findPath(nodes, edges, "C", "A")
        assertEquals(listOf("C", "B", "A"), result.map { it.id })
    }

    // ── No path cases ─────────────────────────────────────────────────────────

    @Test
    fun `findPath returns empty when start node does not exist`() {
        val result = IndoorPathfinder.findPath(nodes, edges, "MISSING", "C")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `findPath returns empty when end node does not exist`() {
        val result = IndoorPathfinder.findPath(nodes, edges, "A", "MISSING")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `findPath returns empty when graph is disconnected`() {
        val isolated = node("Z", 5f, 5f)
        val result = IndoorPathfinder.findPath(nodes + isolated, edges, "A", "Z")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `findPath returns empty list when no edges exist`() {
        val result = IndoorPathfinder.findPath(nodes, emptyList(), "A", "C")
        assertTrue(result.isEmpty())
    }

    // ── Accessibility filtering ───────────────────────────────────────────────

    @Test
    fun `findPath respects accessibleOnly flag - skips inaccessible edges`() {
        // Only B→D is accessible, B→C is not
        val mixedEdges = listOf(
            edge("A", "B", accessible = true),
            edge("B", "C", accessible = false),
            edge("B", "D", accessible = true)
        )
        // Should not reach C because A→B→C has inaccessible edge
        val result = IndoorPathfinder.findPath(nodes, mixedEdges, "A", "C", accessibleOnly = true)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `findPath uses inaccessible edges when accessibleOnly is false`() {
        val mixedEdges = listOf(
            edge("A", "B", accessible = true),
            edge("B", "C", accessible = false)
        )
        val result = IndoorPathfinder.findPath(nodes, mixedEdges, "A", "C", accessibleOnly = false)
        assertEquals(listOf("A", "B", "C"), result.map { it.id })
    }

    // ── Weight / shortest path ────────────────────────────────────────────────

    @Test
    fun `findPath chooses lowest weight path when multiple routes exist`() {
        // Two routes from A to C:
        // A→B→C (weight 1+1=2) vs A→X→C (weight 10+10=20)
        val nodeX = node("X", 0.5f, 0.5f)
        val weightedNodes = nodes + nodeX
        val weightedEdges = listOf(
            edge("A", "B", weight = 1f),
            edge("B", "C", weight = 1f),
            edge("A", "X", weight = 10f),
            edge("X", "C", weight = 10f)
        )
        val result = IndoorPathfinder.findPath(weightedNodes, weightedEdges, "A", "C")
        assertEquals(listOf("A", "B", "C"), result.map { it.id })
    }

    // ── Edge cases ────────────────────────────────────────────────────────────

    @Test
    fun `findPath handles single edge graph`() {
        val twoNodes = listOf(node("S", 0f, 0f), node("E", 1f, 0f))
        val oneEdge  = listOf(edge("S", "E"))
        val result = IndoorPathfinder.findPath(twoNodes, oneEdge, "S", "E")
        assertEquals(listOf("S", "E"), result.map { it.id })
    }

    @Test
    fun `findPath with empty nodes returns empty`() {
        val result = IndoorPathfinder.findPath(emptyList(), emptyList(), "A", "B")
        assertTrue(result.isEmpty())
    }
}
