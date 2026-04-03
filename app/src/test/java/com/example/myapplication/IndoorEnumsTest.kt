package com.example.myapplication.data.indoor

import org.junit.Assert.assertEquals
import org.junit.Test

class IndoorEnumsTest {

    @Test
    fun `RoomType fromRaw matches known values case insensitively`() {
        assertEquals(RoomType.CLASSROOM, RoomType.fromRaw("classroom"))
        assertEquals(RoomType.WASHROOM, RoomType.fromRaw("WASHROOM"))
        assertEquals(RoomType.ESCALATOR, RoomType.fromRaw("Escalator"))
    }

    @Test
    fun `RoomType fromRaw falls back to OTHER for unknown values`() {
        assertEquals(RoomType.OTHER, RoomType.fromRaw("lab"))
        assertEquals(RoomType.OTHER, RoomType.fromRaw(""))
    }

    @Test
    fun `NodeType fromRaw matches known values case insensitively`() {
        assertEquals(NodeType.CORRIDOR, NodeType.fromRaw("corridor"))
        assertEquals(NodeType.ELEVATOR, NodeType.fromRaw("ELEVATOR"))
        assertEquals(NodeType.STAIRCASE, NodeType.fromRaw("Staircase"))
    }

    @Test
    fun `NodeType fromRaw falls back to CORRIDOR for unknown values`() {
        assertEquals(NodeType.CORRIDOR, NodeType.fromRaw("door"))
        assertEquals(NodeType.CORRIDOR, NodeType.fromRaw(""))
    }
}
