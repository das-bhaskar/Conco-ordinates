package com.example.myapplication

import com.example.myapplication.data.indoor.IndoorJsonParser
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [IndoorJsonParser].
 *
 * Validates parsing of raw JSONObjects into IndoorFloor domain models
 * without requiring Android Context or file I/O.
 */
class IndoorJsonParserTest {

    private lateinit var parser: IndoorJsonParser

    @Before
    fun setup() {
        parser = IndoorJsonParser()
    }

    // ── Minimal / empty input ─────────────────────────────────────────────────

    @Test
    fun `parse empty object produces defaults`() {
        val floor = parser.parse(JSONObject())
        assertEquals("", floor.building)
        assertEquals(1, floor.floor)
        assertTrue(floor.rooms.isEmpty())
        assertTrue(floor.corridors.isEmpty())
        assertTrue(floor.nodes.isEmpty())
        assertTrue(floor.edges.isEmpty())
        assertTrue(floor.pois.isEmpty())
        assertTrue(floor.entrances.isEmpty())
    }

    @Test
    fun `parse reads building and floor`() {
        val json = JSONObject()
            .put("building", "H")
            .put("floor", 8)
        val floor = parser.parse(json)
        assertEquals("H", floor.building)
        assertEquals(8, floor.floor)
    }

    // ── Rooms ─────────────────────────────────────────────────────────────────

    @Test
    fun `parse single room with all fields`() {
        val roomJson = JSONObject()
            .put("id", "H-820")
            .put("type", "classroom")
            .put("label", "Room 820")
            .put("icon", "📚")
            .put("accessible", true)
            .put("polygon", JSONArray()
                .put(JSONArray().put(0.1).put(0.2))
                .put(JSONArray().put(0.3).put(0.4))
            )

        val json = JSONObject()
            .put("building", "H")
            .put("floor", 8)
            .put("rooms", JSONArray().put(roomJson))

        val floor = parser.parse(json)
        assertEquals(1, floor.rooms.size)
        val room = floor.rooms[0]
        assertEquals("H-820", room.id)
        assertEquals("classroom", room.type)
        assertEquals("Room 820", room.label)
        assertEquals("📚", room.icon)
        assertTrue(room.accessible)
        assertEquals(2, room.polygon.size)
        assertEquals(0.1f, room.polygon[0].x, 0.001f)
        assertEquals(0.2f, room.polygon[0].y, 0.001f)
    }

    @Test
    fun `parse room with missing optional fields uses defaults`() {
        val roomJson = JSONObject()
            .put("id", "H-821")
            .put("polygon", JSONArray())

        val json = JSONObject()
            .put("rooms", JSONArray().put(roomJson))

        val floor = parser.parse(json)
        val room = floor.rooms[0]
        assertEquals("H-821", room.id)
        assertEquals("other", room.type)
        assertEquals("H-821", room.label) // defaults to id
        assertNull(room.icon)             // blank icon → null
        assertTrue(room.accessible)       // default true
        assertTrue(room.polygon.isEmpty())
    }

    // ── Corridors ─────────────────────────────────────────────────────────────

    @Test
    fun `parse corridor`() {
        val corridorJson = JSONObject()
            .put("id", "corridor-1")
            .put("polygon", JSONArray()
                .put(JSONArray().put(0.0).put(0.0))
                .put(JSONArray().put(1.0).put(1.0))
            )

        val json = JSONObject()
            .put("corridors", JSONArray().put(corridorJson))

        val floor = parser.parse(json)
        assertEquals(1, floor.corridors.size)
        assertEquals("corridor-1", floor.corridors[0].id)
        assertEquals(2, floor.corridors[0].polygon.size)
    }

    // ── Nodes ─────────────────────────────────────────────────────────────────

    @Test
    fun `parse node with all fields`() {
        val nodeJson = JSONObject()
            .put("id", "node-1")
            .put("x", 50.5)
            .put("y", 100.25)
            .put("type", "ELEVATOR")
            .put("roomId", "H-820")
            .put("elevatorGroupId", "elevator-A")
            .put("transferFloor", 9)
            .put("transferNodeId", "node-9-1")
            .put("accessible", false)

        val json = JSONObject()
            .put("nodes", JSONArray().put(nodeJson))

        val floor = parser.parse(json)
        assertEquals(1, floor.nodes.size)
        val node = floor.nodes[0]
        assertEquals("node-1", node.id)
        assertEquals(50.5f, node.x, 0.01f)
        assertEquals(100.25f, node.y, 0.01f)
        assertEquals("ELEVATOR", node.type)
        assertEquals("H-820", node.roomId)
        assertEquals("elevator-A", node.elevatorGroupId)
        assertEquals(9, node.transferFloor)
        assertEquals("node-9-1", node.transferNodeId)
        assertFalse(node.accessible)
    }

    @Test
    fun `parse node with defaults`() {
        val nodeJson = JSONObject()
            .put("id", "node-2")

        val json = JSONObject()
            .put("nodes", JSONArray().put(nodeJson))

        val floor = parser.parse(json)
        val node = floor.nodes[0]
        assertEquals("node-2", node.id)
        assertEquals(0.0f, node.x, 0.01f)
        assertEquals(0.0f, node.y, 0.01f)
        assertEquals("CORRIDOR", node.type)
        assertNull(node.roomId)
        assertNull(node.elevatorGroupId)
        assertNull(node.transferFloor)
        assertNull(node.transferNodeId)
        assertTrue(node.accessible)
    }

    // ── Edges ─────────────────────────────────────────────────────────────────

    @Test
    fun `parse edge with all fields`() {
        val edgeJson = JSONObject()
            .put("from", "node-1")
            .put("to", "node-2")
            .put("weight", 2.5)
            .put("surface", "ROUGH")
            .put("accessible", false)

        val json = JSONObject()
            .put("edges", JSONArray().put(edgeJson))

        val floor = parser.parse(json)
        assertEquals(1, floor.edges.size)
        val edge = floor.edges[0]
        assertEquals("node-1", edge.from)
        assertEquals("node-2", edge.to)
        assertEquals(2.5f, edge.weight, 0.01f)
        assertEquals("ROUGH", edge.surface)
        assertFalse(edge.accessible)
    }

    @Test
    fun `parse edge with defaults`() {
        val edgeJson = JSONObject()
            .put("from", "a")
            .put("to", "b")

        val json = JSONObject()
            .put("edges", JSONArray().put(edgeJson))

        val edge = parser.parse(json).edges[0]
        assertEquals(1.0f, edge.weight, 0.01f)
        assertEquals("SMOOTH", edge.surface)
        assertTrue(edge.accessible)
    }

    // ── POIs ──────────────────────────────────────────────────────────────────

    @Test
    fun `parse poi`() {
        val poiJson = JSONObject()
            .put("id", "poi-1")
            .put("type", "washroom")
            .put("label", "Men's Washroom")
            .put("x", 10.0)
            .put("y", 20.0)
            .put("nodeId", "node-5")

        val json = JSONObject()
            .put("pois", JSONArray().put(poiJson))

        val floor = parser.parse(json)
        assertEquals(1, floor.pois.size)
        val poi = floor.pois[0]
        assertEquals("poi-1", poi.id)
        assertEquals("washroom", poi.type)
        assertEquals("Men's Washroom", poi.label)
        assertEquals(10.0f, poi.x, 0.01f)
        assertEquals(20.0f, poi.y, 0.01f)
        assertEquals("node-5", poi.nodeId)
    }

    // ── Entrances ─────────────────────────────────────────────────────────────

    @Test
    fun `parse entrance`() {
        val entranceJson = JSONObject()
            .put("id", "entrance-1")
            .put("label", "Main Entrance")
            .put("x", 5.0)
            .put("y", 15.0)
            .put("nodeId", "node-10")
            .put("floor", 1)

        val json = JSONObject()
            .put("entrances", JSONArray().put(entranceJson))

        val floor = parser.parse(json)
        assertEquals(1, floor.entrances.size)
        val entrance = floor.entrances[0]
        assertEquals("entrance-1", entrance.id)
        assertEquals("Main Entrance", entrance.label)
        assertEquals(5.0f, entrance.x, 0.01f)
        assertEquals(15.0f, entrance.y, 0.01f)
        assertEquals("node-10", entrance.nodeId)
        assertEquals(1, entrance.floor)
    }

    @Test
    fun `parse entrance with defaults`() {
        val entranceJson = JSONObject()
            .put("id", "entrance-2")

        val json = JSONObject()
            .put("entrances", JSONArray().put(entranceJson))

        val entrance = parser.parse(json).entrances[0]
        assertEquals("entrance-2", entrance.id)
        assertEquals("", entrance.label)
        assertEquals(0.0f, entrance.x, 0.01f)
        assertEquals(0.0f, entrance.y, 0.01f)
        assertEquals("", entrance.nodeId)
        assertEquals(1, entrance.floor)
    }

    // ── Full floor ────────────────────────────────────────────────────────────

    @Test
    fun `parse complete floor JSON`() {
        val json = JSONObject()
            .put("building", "MB")
            .put("floor", 1)
            .put("rooms", JSONArray().put(
                JSONObject().put("id", "MB-S1.401").put("type", "classroom")
                    .put("polygon", JSONArray()
                        .put(JSONArray().put(0.0).put(0.0))
                        .put(JSONArray().put(1.0).put(0.0))
                        .put(JSONArray().put(1.0).put(1.0))
                    )
            ))
            .put("corridors", JSONArray().put(
                JSONObject().put("id", "c1").put("polygon", JSONArray())
            ))
            .put("nodes", JSONArray()
                .put(JSONObject().put("id", "n1").put("x", 10).put("y", 20))
                .put(JSONObject().put("id", "n2").put("x", 30).put("y", 40))
            )
            .put("edges", JSONArray().put(
                JSONObject().put("from", "n1").put("to", "n2").put("weight", 1.5)
            ))
            .put("pois", JSONArray().put(
                JSONObject().put("id", "p1").put("type", "washroom").put("label", "WC")
                    .put("x", 5).put("y", 5)
            ))
            .put("entrances", JSONArray().put(
                JSONObject().put("id", "e1").put("label", "Main").put("x", 0).put("y", 0)
                    .put("nodeId", "n1").put("floor", 1)
            ))

        val floor = parser.parse(json)
        assertEquals("MB", floor.building)
        assertEquals(1, floor.floor)
        assertEquals(1, floor.rooms.size)
        assertEquals(1, floor.corridors.size)
        assertEquals(2, floor.nodes.size)
        assertEquals(1, floor.edges.size)
        assertEquals(1, floor.pois.size)
        assertEquals(1, floor.entrances.size)
    }

    // ── Multiple items ────────────────────────────────────────────────────────

    @Test
    fun `parse multiple rooms`() {
        val rooms = JSONArray()
            .put(JSONObject().put("id", "r1").put("polygon", JSONArray()))
            .put(JSONObject().put("id", "r2").put("polygon", JSONArray()))
            .put(JSONObject().put("id", "r3").put("polygon", JSONArray()))

        val json = JSONObject().put("rooms", rooms)
        assertEquals(3, parser.parse(json).rooms.size)
    }
}
