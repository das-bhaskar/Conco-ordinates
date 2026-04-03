package com.example.myapplication.logic

import androidx.compose.ui.geometry.Offset
import com.example.myapplication.data.indoor.IIndoorRepository
import com.example.myapplication.data.indoor.IndoorFloor
import com.example.myapplication.data.indoor.IndoorNode
import com.example.myapplication.data.indoor.IndoorRoom
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class IndoorRoomResolverDefaultsTest {

    private fun room(id: String, label: String) = IndoorRoom(
        id = id,
        type = "classroom",
        label = label,
        polygon = listOf(Offset(0.5f, 0.5f))
    )

    private fun node(id: String, roomId: String? = null) =
        IndoorNode(id = id, x = 0.5f, y = 0.5f, type = "ROOM", roomId = roomId)

    @Test
    fun `resolve uses default building floors when floors argument is omitted`() = runTest {
        val repo = mock<IIndoorRepository>()
        whenever(repo.getFloor(any(), any())).thenReturn(null)

        val floor8 = IndoorFloor(
            building = "H",
            floor = 8,
            rooms = listOf(room("H-8-829", "H-829")),
            nodes = listOf(node("node-829", roomId = "H-8-829"))
        )
        whenever(repo.getFloor("H", 8)).thenReturn(floor8)

        val result = IndoorRoomResolver.resolve(repo, "H", "829")

        assertNotNull(result)
        assertEquals(8, result!!.floor)
        assertEquals("node-829", result.nodeId)
    }

    @Test
    fun `resolve returns null for unknown building when default floor list is empty`() = runTest {
        val repo = mock<IIndoorRepository>()
        whenever(repo.getFloor(any(), any())).thenReturn(null)

        val result = IndoorRoomResolver.resolve(repo, "UNKNOWN", "101")

        assertNull(result)
    }
}
