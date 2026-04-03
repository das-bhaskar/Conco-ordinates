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

class IndoorOutdoorRouterAdditionalTest {

    private fun node(id: String, x: Float, y: Float, type: String = "CORRIDOR", groupId: String? = null) =
        IndoorNode(id = id, x = x, y = y, type = type, elevatorGroupId = groupId)

    private fun edge(from: String, to: String) = IndoorEdge(from = from, to = to)

    private val floor1 = IndoorFloor(
        building = "H",
        floor = 1,
        nodes = listOf(
            node("start", 0f, 0f),
            node("dest", 1f, 0f),
            node("el_f1", 2f, 0f, "ELEVATOR", "EL-A")
        ),
        edges = listOf(edge("start", "dest"), edge("dest", "el_f1"))
    )

    private val floor8 = IndoorFloor(
        building = "H",
        floor = 8,
        nodes = listOf(
            node("el_f8", 0f, 0f, "ELEVATOR", "EL-A"),
            node("dest_f8", 1f, 0f)
        ),
        edges = listOf(edge("el_f8", "dest_f8"))
    )

    private val ccFloor1 = IndoorFloor(
        building = "CC",
        floor = 1,
        nodes = listOf(node("cc-ent", 0f, 0f), node("cc-dest", 1f, 0f)),
        edges = listOf(edge("cc-ent", "cc-dest"))
    )

    private val hEntrance = BuildingEntrance("node-h-ent", "H South Entrance", LatLng(45.496, -73.579), 1)
    private val ccEntrance = BuildingEntrance("cc-ent", "CC East Entrance", LatLng(45.458, -73.640), 1)

    private fun makeRepo(): IIndoorRepository {
        val repo = mock<IIndoorRepository>()
        runBlocking {
            whenever(repo.getFloor(any(), any())).thenReturn(null)
            whenever(repo.getFloor("H", 1)).thenReturn(floor1)
            whenever(repo.getFloor("H", 8)).thenReturn(floor8)
            whenever(repo.getFloor("CC", 1)).thenReturn(ccFloor1)
        }
        return repo
    }

    @Test
    fun `buildRoute same floor uses destination label in walk instruction`() = runTest {
        val route = IndoorOutdoorRouter.buildRoute(
            repo = makeRepo(),
            startBuilding = "H",
            startFloor = 1,
            startNodeId = "start",
            destination = IndoorOutdoorRouter.IndoorDestination("H", 1, "dest", "Hall Destination"),
            userGps = null,
            entrances = BuildingEntrances(mapOf("H" to listOf(hEntrance)))
        )

        val walk = route.segments.single() as IndoorOutdoorRouter.Segment.IndoorWalk
        assertEquals("Walk to Hall Destination", walk.instruction)
    }

    @Test
    fun `buildRoute cross floor includes elevator instructions`() = runTest {
        val route = IndoorOutdoorRouter.buildRoute(
            repo = makeRepo(),
            startBuilding = "H",
            startFloor = 1,
            startNodeId = "start",
            destination = IndoorOutdoorRouter.IndoorDestination("H", 8, "dest_f8", "H-829"),
            userGps = null,
            entrances = BuildingEntrances(mapOf("H" to listOf(hEntrance)))
        )

        val firstWalk = route.segments[0] as IndoorOutdoorRouter.Segment.IndoorWalk
        val change = route.segments[1] as IndoorOutdoorRouter.Segment.FloorChange
        val finalWalk = route.segments[2] as IndoorOutdoorRouter.Segment.IndoorWalk

        assertEquals("Walk to the elevator", firstWalk.instruction)
        assertEquals("Take the elevator to floor 8", change.instruction)
        assertEquals("Walk to H-829", finalWalk.instruction)
    }

    @Test
    fun `buildRoute multi building uses building name in outdoor instruction`() = runTest {
        val route = IndoorOutdoorRouter.buildRoute(
            repo = makeRepo(),
            startBuilding = "H",
            startFloor = 1,
            startNodeId = "start",
            destination = IndoorOutdoorRouter.IndoorDestination("CC", 1, "cc-dest", "CC-111"),
            userGps = null,
            entrances = BuildingEntrances(mapOf("H" to listOf(hEntrance), "CC" to listOf(ccEntrance)))
        )

        val outdoor = route.segments.filterIsInstance<IndoorOutdoorRouter.Segment.OutdoorWalk>().first()
        assertEquals("Walk to CC building", outdoor.instruction)
    }

    @Test
    fun `buildRoute with missing start building exit still adds destination indoor segment`() = runTest {
        val route = IndoorOutdoorRouter.buildRoute(
            repo = makeRepo(),
            startBuilding = "H",
            startFloor = 1,
            startNodeId = "start",
            destination = IndoorOutdoorRouter.IndoorDestination("CC", 1, "cc-dest", "CC-111"),
            userGps = null,
            entrances = BuildingEntrances(mapOf("CC" to listOf(ccEntrance)))
        )

        assertTrue(route.segments.none { it is IndoorOutdoorRouter.Segment.OutdoorWalk })
        assertTrue(route.segments.any { it is IndoorOutdoorRouter.Segment.IndoorWalk })
    }
}
