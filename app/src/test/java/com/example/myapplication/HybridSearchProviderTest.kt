package com.example.myapplication.logic

import com.example.myapplication.data.Building
import com.example.myapplication.data.Campus
import com.example.myapplication.data.CampusRepo
import com.example.myapplication.data.indoor.IIndoorRepository
import com.example.myapplication.data.indoor.IndoorBuildingConfig
import com.example.myapplication.data.indoor.IndoorCorridor
import com.example.myapplication.data.indoor.IndoorEdge
import com.example.myapplication.data.indoor.IndoorEntrance
import com.example.myapplication.data.indoor.IndoorFloor
import com.example.myapplication.data.indoor.IndoorNode
import com.example.myapplication.data.indoor.IndoorPoi
import com.example.myapplication.data.indoor.IndoorRoom
import com.example.myapplication.telemetry.CrashReporter
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Tasks
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse
import com.google.android.libraries.places.api.net.PlacesClient
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.Runs
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HybridSearchProviderTest {

    private lateinit var placesClient: PlacesClient
    private lateinit var indoorRepo: IIndoorRepository
    private lateinit var provider: HybridSearchProvider

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)

        placesClient = mockk(relaxed = true)
        indoorRepo = mockk()

        mockkObject(CampusRepo)
        mockkObject(CrashReporter)
        mockkObject(IndoorBuildingConfig)

        every { CrashReporter.setKey(any<String>(), any<Int>()) } answers { Unit }
        every { CrashReporter.recordNonFatal(any<Throwable>(), any<String>()) } answers { Unit }
                provider = HybridSearchProvider(placesClient, indoorRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
        unmockkAll()
    }

    @Test
    fun `search returns current location and home when query is blank`() = runTest {
        val result = provider.search("   ")

        assertEquals(2, result.size)
        assertTrue(result[0] == SearchResult.CurrentLocation)
        assertTrue(result[1] == SearchResult.Home)
    }



    @Test
    fun `searchIndoorRooms returns empty when query format is invalid`() = runTest {
        val result = provider.searchIndoorRooms("hall building")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `searchIndoorRooms returns empty when building has no configured floors`() = runTest {
        every { IndoorBuildingConfig.floorsFor("XYZ") } returns emptyList()

        val result = provider.searchIndoorRooms("XYZ-101")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `searchIndoorRooms matches by room label and room id across floors`() = runTest {
        every { IndoorBuildingConfig.floorsFor("H") } returns listOf(1, 8)

        coEvery { indoorRepo.getFloor("H", 1) } returns indoorFloor(
            building = "H",
            floor = 1,
            rooms = listOf(
                IndoorRoom(
                    id = "room_h_112",
                    type = "classroom",
                    label = "H-112",
                    icon = null,
                    polygon = emptyList(),
                    accessible = true
                )
            ),
            nodes = listOf(
                IndoorNode(
                    id = "node-112-f1",
                    x = 0f,
                    y = 0f,
                    type = "ROOM",
                    roomId = "room_h_112",
                    elevatorGroupId = null,
                    transferFloor = null,
                    transferNodeId = null,
                    accessible = true
                )
            )
        )

        coEvery { indoorRepo.getFloor("H", 8) } returns indoorFloor(
            building = "H",
            floor = 8,
            rooms = listOf(
                IndoorRoom(
                    id = "abc112",
                    type = "lab",
                    label = "Computer Lab 112",
                    icon = null,
                    polygon = emptyList(),
                    accessible = true
                )
            ),
            nodes = listOf(
                IndoorNode(
                    id = "node-112-f8",
                    x = 0f,
                    y = 0f,
                    type = "ROOM",
                    roomId = "abc112",
                    elevatorGroupId = null,
                    transferFloor = null,
                    transferNodeId = null,
                    accessible = true
                )
            )
        )

        val result = provider.searchIndoorRooms("H-112")

        assertEquals(2, result.size)

        val first = result[0]
        val second = result[1]

        assertEquals("H", first.buildingCode)
        assertEquals(1, first.floor)
        assertEquals("room_h_112", first.roomId)
        assertEquals("node-112-f1", first.nodeId)

        assertEquals("H", second.buildingCode)
        assertEquals(8, second.floor)
        assertEquals("abc112", second.roomId)
        assertEquals("node-112-f8", second.nodeId)
    }

    @Test
    fun `searchIndoorRooms supports format without dash and lowercase building code`() = runTest {
        every { IndoorBuildingConfig.floorsFor("CC") } returns listOf(1)
        coEvery { indoorRepo.getFloor("CC", 1) } returns indoorFloor(
            building = "CC",
            floor = 1,
            rooms = listOf(
                IndoorRoom(
                    id = "cc119",
                    type = "classroom",
                    label = "CC-119",
                    icon = null,
                    polygon = emptyList(),
                    accessible = true
                )
            ),
            nodes = emptyList()
        )

        val result = provider.searchIndoorRooms("cc119")

        assertEquals(1, result.size)
        assertEquals("CC", result[0].buildingCode)
        assertEquals(1, result[0].floor)
        assertEquals("cc119", result[0].roomId)
        assertNull(result[0].nodeId)
        assertEquals("CC-119 · CC Floor 1", result[0].label)
    }

    @Test
    fun `searchIndoorRooms limits results to three`() = runTest {
        every { IndoorBuildingConfig.floorsFor("H") } returns listOf(1, 2, 3, 4)

        coEvery { indoorRepo.getFloor("H", 1) } returns floorWithSingleRoom("H", 1, "r1", "H-112", "n1")
        coEvery { indoorRepo.getFloor("H", 2) } returns floorWithSingleRoom("H", 2, "r2", "H-112", "n2")
        coEvery { indoorRepo.getFloor("H", 3) } returns floorWithSingleRoom("H", 3, "r3", "H-112", "n3")
        coEvery { indoorRepo.getFloor("H", 4) } returns floorWithSingleRoom("H", 4, "r4", "H-112", "n4")

        val result = provider.searchIndoorRooms("H112")

        assertEquals(3, result.size)
        assertEquals(listOf(1, 2, 3), result.map { it.floor })
    }

    @Test
    fun `displayName returns expected text for each result type`() {
        val building = mockk<Building>()
        val campus = mockk<Campus>()

        every { building.name } returns "Hall Building"
        every { campus.name } returns "SGW Campus"

        assertEquals("Hall Building", SearchResult.BuildingResult(building).displayName)
        assertEquals("SGW Campus", SearchResult.CampusResult(campus).displayName)
        assertEquals("Cafe", SearchResult.GoogleResult("Cafe", "123 St", "p1").displayName)
        assertEquals(
            "H-829 · H Floor 8",
            SearchResult.IndoorRoomResult("H", 8, "r1", "n1", "H-829 · H Floor 8").displayName
        )
        assertEquals("Your position", SearchResult.CurrentLocation.displayName)
        assertEquals("Home", SearchResult.Home.displayName)
    }

    @Test
    fun `coordinates returns expected values for supported result types`() {
        val current = LatLng(45.5, -73.57)
        val buildingCenter = LatLng(45.497, -73.579)
        val campusCenter = LatLng(45.498, -73.58)

        val building = mockk<Building>()
        val campusBuilding = mockk<Building>()
        val campus = mockk<Campus>()

        every { building.getCenter() } returns buildingCenter
        every { campusBuilding.getCenter() } returns campusCenter
        every { campus.buildings } returns listOf(campusBuilding)

        assertEquals(buildingCenter, SearchResult.BuildingResult(building).coordinates(current))
        assertEquals(campusCenter, SearchResult.CampusResult(campus).coordinates(current))
        assertEquals(current, SearchResult.CurrentLocation.coordinates(current))
        assertEquals(
            LatLng(45.51723868665001, -73.627297124046),
            SearchResult.Home.coordinates(current)
        )
        assertNull(SearchResult.GoogleResult("Cafe", "123 St", "p1").coordinates(current))
        assertNull(
            SearchResult.IndoorRoomResult("H", 8, "r1", "n1", "H-829 · H Floor 8")
                .coordinates(current)
        )
    }

    private fun floorWithSingleRoom(
        building: String,
        floor: Int,
        roomId: String,
        label: String,
        nodeId: String
    ): IndoorFloor = indoorFloor(
        building = building,
        floor = floor,
        rooms = listOf(
            IndoorRoom(
                id = roomId,
                type = "classroom",
                label = label,
                icon = null,
                polygon = emptyList(),
                accessible = true
            )
        ),
        nodes = listOf(
            IndoorNode(
                id = nodeId,
                x = 0f,
                y = 0f,
                type = "ROOM",
                roomId = roomId,
                elevatorGroupId = null,
                transferFloor = null,
                transferNodeId = null,
                accessible = true
            )
        )
    )

    private fun indoorFloor(
        building: String,
        floor: Int,
        rooms: List<IndoorRoom>,
        nodes: List<IndoorNode>
    ) = IndoorFloor(
        building = building,
        floor = floor,
        rooms = rooms,
        corridors = emptyList<IndoorCorridor>(),
        nodes = nodes,
        edges = emptyList<IndoorEdge>(),
        pois = emptyList<IndoorPoi>(),
        entrances = emptyList<IndoorEntrance>()
    )
}