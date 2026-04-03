package com.example.myapplication.logic

import com.example.myapplication.data.indoor.BuildingEntrance
import com.example.myapplication.data.indoor.BuildingEntrances
import com.example.myapplication.data.indoor.IIndoorRepository
import com.example.myapplication.data.indoor.IndoorEdge
import com.example.myapplication.data.indoor.IndoorFloor
import com.example.myapplication.data.indoor.IndoorNode
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class IndoorOutdoorRouterEdgeCasesTest {

    private fun node(id: String, x: Float, y: Float, type: String = "CORRIDOR", groupId: String? = null) =
        IndoorNode(id = id, x = x, y = y, type = type, elevatorGroupId = groupId)

    private fun edge(from: String, to: String) = IndoorEdge(from = from, to = to)

    private val hFloor1 = IndoorFloor(
        building = "H",
        floor = 1,
        nodes = listOf(node("start", 0f, 0f), node("exit", 1f, 0f)),
        edges = listOf(edge("start", "exit"))
    )

    private val ccFloor1 = IndoorFloor(
        building = "CC",
        floor = 1,
        nodes = listOf(node("entry", 0f, 0f), node("dest", 1f, 0f)),
        edges = listOf(edge("entry", "dest"))
    )

    private val hEntrance = BuildingEntrance("exit", "H Exit", LatLng(45.496, -73.579), 1)
    private val ccEntrance = BuildingEntrance("entry", "CC Entry", LatLng(45.458, -73.640), 1)

    private fun repo(
        hallFloor: IndoorFloor? = hFloor1,
        ccFloor: IndoorFloor? = ccFloor1
    ): IIndoorRepository {
        val repo = mock<IIndoorRepository>()
        runBlocking {
            whenever(repo.getFloor(any(), any())).thenReturn(null)
            whenever(repo.getFloor("H", 1)).thenReturn(hallFloor)
            whenever(repo.getFloor("CC", 1)).thenReturn(ccFloor)
        }
        return repo
    }

    @Test
    fun `buildRoute different buildings returns empty route when no entrances exist`() = runTest {
        val route = IndoorOutdoorRouter.buildRoute(
            repo = repo(),
            startBuilding = "H",
            startFloor = 1,
            startNodeId = "start",
            destination = IndoorOutdoorRouter.IndoorDestination("CC", 1, "dest", "CC-101"),
            userGps = null,
            entrances = BuildingEntrances()
        )

        assertTrue(route.segments.isEmpty())
    }

    @Test
    fun `buildRoute different buildings returns only outdoor walk when floor data is missing on both sides`() = runTest {
        val route = IndoorOutdoorRouter.buildRoute(
            repo = repo(hallFloor = null, ccFloor = null),
            startBuilding = "H",
            startFloor = 1,
            startNodeId = "start",
            destination = IndoorOutdoorRouter.IndoorDestination("CC", 1, "dest", "CC-101"),
            userGps = null,
            entrances = BuildingEntrances(mapOf("H" to listOf(hEntrance), "CC" to listOf(ccEntrance)))
        )

        assertEquals(1, route.segments.size)
        assertTrue(route.segments.single() is IndoorOutdoorRouter.Segment.OutdoorWalk)
    }

    @Test
    fun `buildRoute different buildings returns start walk plus outdoor walk when destination floor data is missing`() = runTest {
        val route = IndoorOutdoorRouter.buildRoute(
            repo = repo(ccFloor = null),
            startBuilding = "H",
            startFloor = 1,
            startNodeId = "start",
            destination = IndoorOutdoorRouter.IndoorDestination("CC", 1, "dest", "CC-101"),
            userGps = null,
            entrances = BuildingEntrances(mapOf("H" to listOf(hEntrance), "CC" to listOf(ccEntrance)))
        )

        assertEquals(2, route.segments.size)
        assertTrue(route.segments[0] is IndoorOutdoorRouter.Segment.IndoorWalk)
        assertTrue(route.segments[1] is IndoorOutdoorRouter.Segment.OutdoorWalk)
    }
}
