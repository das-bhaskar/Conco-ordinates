package com.example.myapplication.logic

import com.example.myapplication.data.indoor.IIndoorRepository
import com.example.myapplication.data.indoor.IndoorEdge
import com.example.myapplication.data.indoor.IndoorFloor
import com.example.myapplication.data.indoor.IndoorNode
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class CrossFloorNavigatorPreferenceFallbackTest {

    private fun node(
        id: String,
        x: Float,
        y: Float,
        type: String = "CORRIDOR",
        groupId: String? = null
    ) = IndoorNode(id = id, x = x, y = y, type = type, elevatorGroupId = groupId)

    private fun edge(from: String, to: String) = IndoorEdge(from = from, to = to)

    private val floor1 = IndoorFloor(
        building = "H",
        floor = 1,
        nodes = listOf(
            node("start", 0f, 0f),
            node("el_f1", 1f, 0f, type = "ELEVATOR", groupId = "EL-A")
        ),
        edges = listOf(edge("start", "el_f1"))
    )

    private val floor8 = IndoorFloor(
        building = "H",
        floor = 8,
        nodes = listOf(
            node("el_f8", 0f, 0f, type = "ELEVATOR", groupId = "EL-A"),
            node("dest", 1f, 0f)
        ),
        edges = listOf(edge("el_f8", "dest"))
    )

    private fun makeRepo(): IIndoorRepository {
        val repo = mock<IIndoorRepository>()
        runBlocking {
            whenever(repo.getFloor("H", 1)).thenReturn(floor1)
            whenever(repo.getFloor("H", 8)).thenReturn(floor8)
        }
        return repo
    }

    @Test
    fun `navigate with ESCALATOR falls back to elevator when no escalator exists`() = runTest {
        val steps = CrossFloorNavigator.navigate(
            repo = makeRepo(),
            building = "H",
            startFloor = 1,
            startNodeId = "start",
            targetFloor = 8,
            targetNodeId = "dest",
            preference = TransferPreference.ESCALATOR
        )

        assertEquals(3, steps.size)
        val change = steps[1] as CrossFloorNavigator.NavStep.ChangeFloor
        assertEquals(CrossFloorNavigator.VIA_ELEVATOR, change.via)
    }

    @Test
    fun `navigate with STAIRS falls back to elevator when no staircase exists`() = runTest {
        val steps = CrossFloorNavigator.navigate(
            repo = makeRepo(),
            building = "H",
            startFloor = 1,
            startNodeId = "start",
            targetFloor = 8,
            targetNodeId = "dest",
            preference = TransferPreference.STAIRS
        )

        assertTrue(steps.isNotEmpty())
        val change = steps[1] as CrossFloorNavigator.NavStep.ChangeFloor
        assertEquals(CrossFloorNavigator.VIA_ELEVATOR, change.via)
    }
}
