package com.example.myapplication.logic

import androidx.compose.ui.geometry.Offset
import com.example.myapplication.data.indoor.IIndoorRepository
import com.example.myapplication.data.indoor.IndoorFloor
import com.example.myapplication.data.indoor.IndoorNode
import com.example.myapplication.data.indoor.IndoorRoom
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class SearchProviderFormatsTest {

    private lateinit var placesClient: PlacesClient
    private lateinit var indoorRepo: IIndoorRepository
    private lateinit var provider: HybridSearchProvider

    @Before
    fun setup() {
        placesClient = mock()
        indoorRepo = mock()
        provider = HybridSearchProvider(placesClient, indoorRepo)
    }

    private fun room(id: String, label: String) = IndoorRoom(
        id = id,
        type = "classroom",
        label = label,
        polygon = listOf(Offset(0.5f, 0.5f))
    )

    private fun node(id: String, roomId: String? = null) =
        IndoorNode(id = id, x = 0.5f, y = 0.5f, type = "ROOM", roomId = roomId)

    @Test
    fun `searchIndoorRooms supports query without separator`() = runTest {
        val floor = IndoorFloor(
            building = "H",
            floor = 8,
            rooms = listOf(room("H-8-829", "H-829")),
            nodes = listOf(node("node-829", roomId = "H-8-829"))
        )
        whenever(indoorRepo.getFloor(any(), any())).thenReturn(null)
        whenever(indoorRepo.getFloor("H", 8)).thenReturn(floor)

        val results = provider.searchIndoorRooms("H829")

        assertEquals(1, results.size)
        assertEquals("H", results.first().buildingCode)
    }

    @Test
    fun `searchIndoorRooms supports spaces and lowercase building code`() = runTest {
        val floor = IndoorFloor(
            building = "H",
            floor = 8,
            rooms = listOf(room("H-8-829", "H-829")),
            nodes = listOf(node("node-829", roomId = "H-8-829"))
        )
        whenever(indoorRepo.getFloor(any(), any())).thenReturn(null)
        whenever(indoorRepo.getFloor("H", 8)).thenReturn(floor)

        val results = provider.searchIndoorRooms("h 829")

        assertEquals(1, results.size)
        assertEquals("H-8-829", results.first().roomId)
    }

    @Test
    fun `searchIndoorRooms supports hyphenated room suffix`() = runTest {
        val floor = IndoorFloor(
            building = "H",
            floor = 1,
            rooms = listOf(room("H-1-112-2", "H-112-2")),
            nodes = listOf(node("node-112-2", roomId = "H-1-112-2"))
        )
        whenever(indoorRepo.getFloor(any(), any())).thenReturn(null)
        whenever(indoorRepo.getFloor("H", 1)).thenReturn(floor)

        val results = provider.searchIndoorRooms("H-112-2")

        assertEquals(1, results.size)
        assertEquals("node-112-2", results.first().nodeId)
    }

    @Test
    fun `searchIndoorRooms formats result label with building and floor`() = runTest {
        val floor = IndoorFloor(
            building = "CC",
            floor = 1,
            rooms = listOf(room("CC-1-101", "CC-101")),
            nodes = listOf(node("node-101", roomId = "CC-1-101"))
        )
        whenever(indoorRepo.getFloor(any(), any())).thenReturn(null)
        whenever(indoorRepo.getFloor("CC", 1)).thenReturn(floor)

        val results = provider.searchIndoorRooms("CC101")

        assertEquals("CC-101 · CC Floor 1", results.first().label)
    }

    @Test
    fun `searchIndoorRooms returns empty when query does not match indoor pattern`() = runTest {
        val results = provider.searchIndoorRooms("hall building")
        assertTrue(results.isEmpty())
    }
}
