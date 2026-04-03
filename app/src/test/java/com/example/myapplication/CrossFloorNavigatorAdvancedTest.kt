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

class CrossFloorNavigatorAdvancedTest {

    private fun node(
        id: String,
        x: Float,
        y: Float,
        type: String = "CORRIDOR",
        groupId: String? = null
    ) = IndoorNode(id = id, x = x, y = y, type = type, elevatorGroupId = groupId)

    private fun edge(from: String, to: String, accessible: Boolean = true) =
        IndoorEdge(from = from, to = to, accessible = accessible)

    @Test
    fun `navigate with ESCALATOR uses escalator when available`() = runTest {
        val floor1 = IndoorFloor(
            building = "H",
            floor = 1,
            nodes = listOf(
                node("start", 0f, 0f),
                node("es_f1", 1f, 0f, "ESCALATOR", "ES-A"),
                node("el_f1", 3f, 0f, "ELEVATOR", "EL-A")
            ),
            edges = listOf(edge("start", "es_f1"), edge("start", "el_f1"))
        )
        val floor8 = IndoorFloor(
            building = "H",
            floor = 8,
            nodes = listOf(
                node("es_f8", 0f, 0f, "ESCALATOR", "ES-A"),
                node("el_f8", 2f, 0f, "ELEVATOR", "EL-A"),
                node("dest", 1f, 0f)
            ),
            edges = listOf(edge("es_f8", "dest"), edge("el_f8", "dest"))
        )
        val repo = mock<IIndoorRepository>()
        runBlocking {
            whenever(repo.getFloor("H", 1)).thenReturn(floor1)
            whenever(repo.getFloor("H", 8)).thenReturn(floor8)
        }

        val steps = CrossFloorNavigator.navigate(repo, "H", 1, "start", 8, "dest", TransferPreference.ESCALATOR)

        val change = steps[1] as CrossFloorNavigator.NavStep.ChangeFloor
        assertEquals(CrossFloorNavigator.VIA_ESCALATOR, change.via)
    }

    @Test
    fun `navigate with STAIRS uses staircase when available`() = runTest {
        val floor1 = IndoorFloor(
            building = "H",
            floor = 1,
            nodes = listOf(
                node("start", 0f, 0f),
                node("st_f1", 1f, 0f, "STAIRCASE", "ST-A")
            ),
            edges = listOf(edge("start", "st_f1"))
        )
        val floor8 = IndoorFloor(
            building = "H",
            floor = 8,
            nodes = listOf(
                node("st_f8", 0f, 0f, "STAIRCASE", "ST-A"),
                node("dest", 1f, 0f)
            ),
            edges = listOf(edge("st_f8", "dest"))
        )
        val repo = mock<IIndoorRepository>()
        runBlocking {
            whenever(repo.getFloor("H", 1)).thenReturn(floor1)
            whenever(repo.getFloor("H", 8)).thenReturn(floor8)
        }

        val steps = CrossFloorNavigator.navigate(repo, "H", 1, "start", 8, "dest", TransferPreference.STAIRS)

        val change = steps[1] as CrossFloorNavigator.NavStep.ChangeFloor
        assertEquals(CrossFloorNavigator.VIA_STAIRCASE, change.via)
    }

    @Test
    fun `navigate with inaccessible elevator path returns empty in elevator only mode`() = runTest {
        val floor1 = IndoorFloor(
            building = "H",
            floor = 1,
            nodes = listOf(
                node("start", 0f, 0f),
                node("el_f1", 1f, 0f, "ELEVATOR", "EL-A")
            ),
            edges = listOf(edge("start", "el_f1", accessible = false))
        )
        val floor8 = IndoorFloor(
            building = "H",
            floor = 8,
            nodes = listOf(
                node("el_f8", 0f, 0f, "ELEVATOR", "EL-A"),
                node("dest", 1f, 0f)
            ),
            edges = listOf(edge("el_f8", "dest"))
        )
        val repo = mock<IIndoorRepository>()
        runBlocking {
            whenever(repo.getFloor("H", 1)).thenReturn(floor1)
            whenever(repo.getFloor("H", 8)).thenReturn(floor8)
        }

        val steps = CrossFloorNavigator.navigate(repo, "H", 1, "start", 8, "dest", TransferPreference.ELEVATOR_ONLY)

        assertTrue(steps.isEmpty())
    }

    @Test
    fun `navigate ANY chooses the shorter transfer pair`() = runTest {
        val floor1 = IndoorFloor(
            building = "H",
            floor = 1,
            nodes = listOf(
                node("start", 0f, 0f),
                node("el_near_f1", 0.2f, 0f, "ELEVATOR", "EL-NEAR"),
                node("el_far_f1", 0.9f, 0f, "ELEVATOR", "EL-FAR")
            ),
            edges = listOf(edge("start", "el_near_f1"), edge("start", "el_far_f1"))
        )
        val floor8 = IndoorFloor(
            building = "H",
            floor = 8,
            nodes = listOf(
                node("el_near_f8", 0f, 0f, "ELEVATOR", "EL-NEAR"),
                node("el_far_f8", 1f, 0f, "ELEVATOR", "EL-FAR"),
                node("dest", 0.2f, 0f)
            ),
            edges = listOf(edge("el_near_f8", "dest"), edge("el_far_f8", "dest"))
        )
        val repo = mock<IIndoorRepository>()
        runBlocking {
            whenever(repo.getFloor("H", 1)).thenReturn(floor1)
            whenever(repo.getFloor("H", 8)).thenReturn(floor8)
        }

        val steps = CrossFloorNavigator.navigate(repo, "H", 1, "start", 8, "dest", TransferPreference.ANY)

        val change = steps[1] as CrossFloorNavigator.NavStep.ChangeFloor
        assertEquals("el_near_f8", change.targetNodeId)
    }
}
