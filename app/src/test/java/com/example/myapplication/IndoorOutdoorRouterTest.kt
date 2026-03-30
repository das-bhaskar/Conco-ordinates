package com.example.myapplication.logic

import com.example.myapplication.data.indoor.BuildingEntrance
import com.example.myapplication.data.indoor.BuildingEntrances
import com.example.myapplication.data.indoor.IndoorEdge
import com.example.myapplication.data.indoor.IndoorFloor
import com.example.myapplication.data.indoor.IndoorNode
import com.example.myapplication.data.indoor.IndoorRepository
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

/**
 * Unit tests for [IndoorOutdoorRouter].
 *
 * Floor layout used in tests:
 *   Floor 1: start ──── dest     (same floor, same building)
 *   Floor 8: el_f8 ──── dest_f8  (cross-floor)
 */
class IndoorOutdoorRouterTest {

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun node(id: String, x: Float, y: Float, type: String = "CORRIDOR",
                     groupId: String? = null) =
        IndoorNode(id = id, x = x, y = y, type = type, elevatorGroupId = groupId)

    private fun edge(from: String, to: String) = IndoorEdge(from = from, to = to)

    private val startNode  = node("start",  0f, 0f)
    private val destNode   = node("dest",   1f, 0f)
    private val elF1       = node("el_f1",  2f, 0f, "ELEVATOR", "EL-A")
    private val elF8       = node("el_f8",  0f, 0f, "ELEVATOR", "EL-A")
    private val destF8     = node("dest_f8",1f, 0f)

    private val floor1 = IndoorFloor(
        building = "H", floor = 1,
        nodes    = listOf(startNode, destNode, elF1),
        edges    = listOf(edge("start", "dest"), edge("dest", "el_f1"))
    )
    private val floor8 = IndoorFloor(
        building = "H", floor = 8,
        nodes    = listOf(elF8, destF8),
        edges    = listOf(edge("el_f8", "dest_f8"))
    )

    private val hEntrance = BuildingEntrance(
        nodeId = "node-h-ent", label = "H South Entrance",
        gps = LatLng(45.496, -73.579), floor = 1
    )
    private val ccEntrance = BuildingEntrance(
        nodeId = "node-cc-ent", label = "CC East Entrance",
        gps = LatLng(45.458, -73.640), floor = 1
    )

    private val ccFloor1 = IndoorFloor(
        building = "CC", floor = 1,
        nodes    = listOf(node("cc-ent", 0f, 0f), node("cc-dest", 1f, 0f)),
        edges    = listOf(edge("cc-ent", "cc-dest"))
    )

    private fun makeRepo(): IndoorRepository {
        val repo = mock<IndoorRepository>()
        runBlocking { whenever(repo.getFloor(any(), any())).thenReturn(null) }
        runBlocking { whenever(repo.getFloor("H",  1)).thenReturn(floor1) }
        runBlocking { whenever(repo.getFloor("H",  8)).thenReturn(floor8) }
        runBlocking { whenever(repo.getFloor("CC", 1)).thenReturn(ccFloor1) }
        return repo
    }

    @Suppress("UNUSED")
    private fun runBlocking(block: suspend () -> Unit) =
        kotlinx.coroutines.runBlocking { block() }

    @Before
    fun setup() {
        injectEntrances(mapOf(
            "H"  to listOf(hEntrance),
            "CC" to listOf(ccEntrance)
        ))
    }

    private fun injectEntrances(data: Map<String, List<BuildingEntrance>>) {
        val field = BuildingEntrances::class.java.getDeclaredField("data")
        field.isAccessible = true
        field.set(BuildingEntrances.default, data)
    }

    // ── Case 1: same building, same floor ─────────────────────────────────────

    @Test
    fun `buildRoute same building same floor returns single IndoorWalk segment`() = runTest {
        val repo = makeRepo()
        val dest = IndoorOutdoorRouter.IndoorDestination("H", 1, "dest", "H-Dest")

        val route = IndoorOutdoorRouter.buildRoute(
            repo          = repo,
            startBuilding = "H",
            startFloor    = 1,
            startNodeId   = "start",
            destination   = dest,
            userGps       = null
        )

        assertEquals(1, route.segments.size)
        assertTrue(route.segments[0] is IndoorOutdoorRouter.Segment.IndoorWalk)
    }

    @Test
    fun `buildRoute same building same floor returns empty when floor data missing`() = runTest {
        val repo = mock<IndoorRepository>()
        runBlocking { whenever(repo.getFloor(any(), any())).thenReturn(null) }

        val dest = IndoorOutdoorRouter.IndoorDestination("H", 1, "dest", "H-Dest")
        val route = IndoorOutdoorRouter.buildRoute(repo, "H", 1, "start", dest, null)

        assertTrue(route.segments.isEmpty())
    }

    // ── Case 2: same building, different floor ────────────────────────────────

    @Test
    fun `buildRoute same building different floor returns multi-segment route`() = runTest {
        val repo = makeRepo()
        val dest = IndoorOutdoorRouter.IndoorDestination("H", 8, "dest_f8", "H-829")

        val route = IndoorOutdoorRouter.buildRoute(
            repo          = repo,
            startBuilding = "H",
            startFloor    = 1,
            startNodeId   = "start",
            destination   = dest,
            userGps       = null
        )

        assertTrue(route.segments.isNotEmpty())
        // Should contain Walk + FloorChange + Walk
        assertTrue(route.segments.any { it is IndoorOutdoorRouter.Segment.IndoorWalk })
        assertTrue(route.segments.any { it is IndoorOutdoorRouter.Segment.FloorChange })
    }

    // ── Case 3: different buildings ───────────────────────────────────────────

    @Test
    fun `buildRoute different buildings includes OutdoorWalk segment`() = runTest {
        val repo = makeRepo()
        val dest = IndoorOutdoorRouter.IndoorDestination("CC", 1, "cc-dest", "CC-111")

        val route = IndoorOutdoorRouter.buildRoute(
            repo          = repo,
            startBuilding = "H",
            startFloor    = 1,
            startNodeId   = "start",
            destination   = dest,
            userGps       = LatLng(45.497, -73.579)
        )

        assertTrue(route.segments.any { it is IndoorOutdoorRouter.Segment.OutdoorWalk })
    }

    @Test
    fun `buildRoute different buildings outdoor segment connects H exit to CC entrance`() = runTest {
        val repo = makeRepo()
        val dest = IndoorOutdoorRouter.IndoorDestination("CC", 1, "cc-dest", "CC-111")

        val route = IndoorOutdoorRouter.buildRoute(
            repo          = repo,
            startBuilding = "H",
            startFloor    = 1,
            startNodeId   = "start",
            destination   = dest,
            userGps       = null
        )

        val outdoor = route.segments.filterIsInstance<IndoorOutdoorRouter.Segment.OutdoorWalk>()
            .firstOrNull()
        assertNotNull(outdoor)
        assertEquals(hEntrance.gps, outdoor?.origin)
        assertEquals(ccEntrance.gps, outdoor?.destination)
    }

    // ── FullRoute ─────────────────────────────────────────────────────────────

    @Test
    fun `FullRoute totalSteps defaults to segments size`() {
        val walk = IndoorOutdoorRouter.Segment.IndoorWalk("H", 1, emptyList(), "Walk")
        val route = IndoorOutdoorRouter.FullRoute(listOf(walk))
        assertEquals(1, route.totalSteps)
    }

    @Test
    fun `FullRoute with multiple segments has correct totalSteps`() {
        val walk1 = IndoorOutdoorRouter.Segment.IndoorWalk("H", 1, emptyList(), "Walk 1")
        val walk2 = IndoorOutdoorRouter.Segment.IndoorWalk("H", 8, emptyList(), "Walk 2")
        val fc    = IndoorOutdoorRouter.Segment.FloorChange("H", 1, 8, "elevator", "Take elevator")
        val route = IndoorOutdoorRouter.FullRoute(listOf(walk1, fc, walk2))
        assertEquals(3, route.totalSteps)
    }

    // ── Case 3 edge cases: multi-building ─────────────────────────────────────

    @Test
    fun `buildRoute different buildings with no CC entrance returns partial route`() = runTest {
        // Clear CC entrance so addDestinationWalkSegments has no bestEntry
        injectEntrances(mapOf("H" to listOf(hEntrance)))
        val repo = makeRepo()
        val dest = IndoorOutdoorRouter.IndoorDestination("CC", 1, "cc-dest", "CC-101")
        val route = IndoorOutdoorRouter.buildRoute(
            repo, "H", 1, "start", dest,
            userGps = LatLng(45.496, -73.579),
            preference = TransferPreference.ANY
        )
        // No CC entrance → no outdoor or indoor segments to CC
        assertTrue(route.segments.none { it is IndoorOutdoorRouter.Segment.OutdoorWalk })
    }

    @Test
    fun `buildRoute different buildings with userGps picks nearest exit`() = runTest {
        val repo = makeRepo()
        val dest = IndoorOutdoorRouter.IndoorDestination("CC", 1, "cc-dest", "CC-101")
        // User near H entrance
        val route = IndoorOutdoorRouter.buildRoute(
            repo, "H", 1, "start", dest,
            userGps = LatLng(45.496, -73.579),
            preference = TransferPreference.ANY
        )
        // Should have an outdoor walk since CC entrance exists
        assertTrue(route.segments.any { it is IndoorOutdoorRouter.Segment.OutdoorWalk })
    }

    @Test
    fun `buildRoute different buildings with null userGps still works`() = runTest {
        val repo = makeRepo()
        val dest = IndoorOutdoorRouter.IndoorDestination("CC", 1, "cc-dest", "CC-101")
        val route = IndoorOutdoorRouter.buildRoute(
            repo, "H", 1, "start", dest,
            userGps = null,
            preference = TransferPreference.ANY
        )
        assertNotNull(route)
    }

    @Test
    fun `buildRoute same building cross-floor with ELEVATOR_ONLY preference`() = runTest {
        val repo = makeRepo()
        val dest = IndoorOutdoorRouter.IndoorDestination("H", 8, "dest_f8", "H-829")
        val route = IndoorOutdoorRouter.buildRoute(
            repo, "H", 1, "start", dest,
            userGps = null,
            preference = TransferPreference.ELEVATOR_ONLY
        )
        // Elevator path: walk → floor change → walk
        assertTrue(route.segments.isNotEmpty())
    }

    // ── Segment data classes ──────────────────────────────────────────────────

    @Test
    fun `IndoorWalk segment data class equality`() {
        val s1 = IndoorOutdoorRouter.Segment.IndoorWalk("H", 1, emptyList(), "Walk")
        val s2 = s1.copy()
        assertEquals(s1, s2)
        assertNotEquals(s1, s1.copy(floor = 8))
    }

    @Test
    fun `FloorChange segment data class equality`() {
        val s1 = IndoorOutdoorRouter.Segment.FloorChange("H", 1, 8, "elevator", "instruction")
        val s2 = s1.copy()
        assertEquals(s1, s2)
        assertNotEquals(s1, s1.copy(toFloor = 9))
    }

    @Test
    fun `OutdoorWalk segment data class equality`() {
        val gps1 = LatLng(45.497, -73.579)
        val gps2 = LatLng(45.458, -73.640)
        val s1 = IndoorOutdoorRouter.Segment.OutdoorWalk(gps1, gps2, "H Exit", "CC Entry", "Walk")
        val s2 = s1.copy()
        assertEquals(s1, s2)
        assertNotNull(s1.toString())
    }

    // ── IndoorDestination ─────────────────────────────────────────────────────

    @Test
    fun `IndoorDestination data class equality`() {
        val d1 = IndoorOutdoorRouter.IndoorDestination("H", 8, "node-829", "H-829")
        val d2 = d1.copy()
        assertEquals(d1, d2)
        assertNotEquals(d1, d1.copy(floor = 1))
        assertNotNull(d1.toString())
    }
}
