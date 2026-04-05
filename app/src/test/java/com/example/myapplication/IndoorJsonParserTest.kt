package com.example.myapplication.data.indoor

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IndoorJsonParserTest {

    private val parser = IndoorJsonParser()

    @Test
    fun `parse should correctly map full json into IndoorFloor`() {
        val json = JSONObject(
            """
            {
              "building": "Hall",
              "floor": 2,
              "rooms": [
                {
                  "id": "room101",
                  "type": "classroom",
                  "label": "Room 101",
                  "icon": "school",
                  "polygon": [[1,2],[3,4],[5,6]],
                  "accessible": false
                }
              ],
              "corridors": [
                {
                  "id": "corr1",
                  "polygon": [[10,20],[30,40]]
                }
              ],
              "nodes": [
                {
                  "id": "node1",
                  "x": 12.5,
                  "y": 15.75,
                  "type": "ELEVATOR",
                  "roomId": "room101",
                  "elevatorGroupId": "e1",
                  "transferFloor": 3,
                  "transferNodeId": "node2",
                  "accessible": false
                }
              ],
              "edges": [
                {
                  "from": "node1",
                  "to": "node2",
                  "weight": 7.5,
                  "surface": "RAMP",
                  "accessible": false
                }
              ],
              "pois": [
                {
                  "id": "poi1",
                  "type": "washroom",
                  "label": "Washroom",
                  "x": 100.0,
                  "y": 200.0,
                  "nodeId": "node1"
                }
              ],
              "entrances": [
                {
                  "id": "ent1",
                  "label": "Main Entrance",
                  "x": 9.0,
                  "y": 11.0,
                  "nodeId": "nodeA",
                  "floor": 2
                }
              ]
            }
            """.trimIndent()
        )

        val result = parser.parse(json)

        assertEquals("Hall", result.building)
        assertEquals(2, result.floor)

        assertEquals(1, result.rooms.size)
        val room = result.rooms.first()
        assertEquals("room101", room.id)
        assertEquals("classroom", room.type)
        assertEquals("Room 101", room.label)
        assertEquals("school", room.icon)
        assertFalse(room.accessible)
        assertEquals(3, room.polygon.size)
        assertEquals(1f, room.polygon[0].x, 0.001f)
        assertEquals(2f, room.polygon[0].y, 0.001f)
        assertEquals(5f, room.polygon[2].x, 0.001f)
        assertEquals(6f, room.polygon[2].y, 0.001f)

        assertEquals(1, result.corridors.size)
        val corridor = result.corridors.first()
        assertEquals("corr1", corridor.id)
        assertEquals(2, corridor.polygon.size)
        assertEquals(10f, corridor.polygon[0].x, 0.001f)
        assertEquals(20f, corridor.polygon[0].y, 0.001f)

        assertEquals(1, result.nodes.size)
        val node = result.nodes.first()
        assertEquals("node1", node.id)
        assertEquals(12.5f, node.x, 0.001f)
        assertEquals(15.75f, node.y, 0.001f)
        assertEquals("ELEVATOR", node.type)
        assertEquals("room101", node.roomId)
        assertEquals("e1", node.elevatorGroupId)
        assertEquals(3, node.transferFloor)
        assertEquals("node2", node.transferNodeId)
        assertFalse(node.accessible)

        assertEquals(1, result.edges.size)
        val edge = result.edges.first()
        assertEquals("node1", edge.from)
        assertEquals("node2", edge.to)
        assertEquals(7.5f, edge.weight, 0.001f)
        assertEquals("RAMP", edge.surface)
        assertFalse(edge.accessible)

        assertEquals(1, result.pois.size)
        val poi = result.pois.first()
        assertEquals("poi1", poi.id)
        assertEquals("washroom", poi.type)
        assertEquals("Washroom", poi.label)
        assertEquals(100f, poi.x, 0.001f)
        assertEquals(200f, poi.y, 0.001f)
        assertEquals("node1", poi.nodeId)

        assertEquals(1, result.entrances.size)
        val entrance = result.entrances.first()
        assertEquals("ent1", entrance.id)
        assertEquals("Main Entrance", entrance.label)
        assertEquals(9f, entrance.x, 0.001f)
        assertEquals(11f, entrance.y, 0.001f)
        assertEquals("nodeA", entrance.nodeId)
        assertEquals(2, entrance.floor)
    }

    @Test
    fun `parse should use defaults when fields are missing`() {
        val json = JSONObject("""{}""")

        val result = parser.parse(json)

        assertEquals("", result.building)
        assertEquals(1, result.floor)
        assertTrue(result.rooms.isEmpty())
        assertTrue(result.corridors.isEmpty())
        assertTrue(result.nodes.isEmpty())
        assertTrue(result.edges.isEmpty())
        assertTrue(result.pois.isEmpty())
        assertTrue(result.entrances.isEmpty())
    }

    @Test
    fun `parseRoom should use fallback label and null icon when blank`() {
        val json = JSONObject(
            """
            {
              "rooms": [
                {
                  "id": "r1",
                  "icon": "   ",
                  "polygon": []
                }
              ]
            }
            """.trimIndent()
        )

        val result = parser.parse(json)
        val room = result.rooms.first()

        assertEquals("r1", room.id)
        assertEquals("other", room.type)
        assertEquals("r1", room.label)
        assertNull(room.icon)
        assertTrue(room.accessible)
        assertTrue(room.polygon.isEmpty())
    }

    @Test
    fun `parseNode should convert blank optional strings to null`() {
        val json = JSONObject(
            """
            {
              "nodes": [
                {
                  "id": "n1",
                  "x": 1.0,
                  "y": 2.0,
                  "roomId": "",
                  "elevatorGroupId": "   ",
                  "transferNodeId": ""
                }
              ]
            }
            """.trimIndent()
        )

        val result = parser.parse(json)
        val node = result.nodes.first()

        assertEquals("n1", node.id)
        assertEquals(1f, node.x, 0.001f)
        assertEquals(2f, node.y, 0.001f)
        assertEquals("CORRIDOR", node.type)
        assertNull(node.roomId)
        assertNull(node.elevatorGroupId)
        assertNull(node.transferNodeId)
        assertNull(node.transferFloor)
        assertTrue(node.accessible)
    }

    @Test
    fun `parseNode should keep transferFloor null when key is absent`() {
        val json = JSONObject(
            """
            {
              "nodes": [
                {
                  "id": "n1",
                  "x": 0.0,
                  "y": 0.0
                }
              ]
            }
            """.trimIndent()
        )

        val result = parser.parse(json)
        val node = result.nodes.first()

        assertNull(node.transferFloor)
    }

    @Test
    fun `parseNode should read transferFloor when present`() {
        val json = JSONObject(
            """
            {
              "nodes": [
                {
                  "id": "n1",
                  "x": 0.0,
                  "y": 0.0,
                  "transferFloor": 5
                }
              ]
            }
            """.trimIndent()
        )

        val result = parser.parse(json)
        val node = result.nodes.first()

        assertNotNull(node.transferFloor)
        assertEquals(5, node.transferFloor)
    }

    @Test
    fun `parseEdge should use default values when fields are missing`() {
        val json = JSONObject(
            """
            {
              "edges": [
                {}
              ]
            }
            """.trimIndent()
        )

        val result = parser.parse(json)
        val edge = result.edges.first()

        assertEquals("", edge.from)
        assertEquals("", edge.to)
        assertEquals(1.0f, edge.weight, 0.001f)
        assertEquals("SMOOTH", edge.surface)
        assertTrue(edge.accessible)
    }

    @Test
    fun `parsePoi and entrance should use defaults when fields are missing`() {
        val json = JSONObject(
            """
            {
              "pois": [{}],
              "entrances": [{}]
            }
            """.trimIndent()
        )

        val result = parser.parse(json)

        val poi = result.pois.first()
        assertEquals("", poi.id)
        assertEquals("other", poi.type)
        assertEquals("", poi.label)
        assertEquals(0f, poi.x, 0.001f)
        assertEquals(0f, poi.y, 0.001f)
        assertEquals("", poi.nodeId)

        val entrance = result.entrances.first()
        assertEquals("", entrance.id)
        assertEquals("", entrance.label)
        assertEquals(0f, entrance.x, 0.001f)
        assertEquals(0f, entrance.y, 0.001f)
        assertEquals("", entrance.nodeId)
        assertEquals(1, entrance.floor)
    }

    @Test
    fun `parse should handle missing section arrays as empty lists`() {
        val json = JSONObject(
            """
            {
              "building": "EV",
              "floor": 1
            }
            """.trimIndent()
        )

        val result = parser.parse(json)

        assertEquals("EV", result.building)
        assertEquals(1, result.floor)
        assertTrue(result.rooms.isEmpty())
        assertTrue(result.corridors.isEmpty())
        assertTrue(result.nodes.isEmpty())
        assertTrue(result.edges.isEmpty())
        assertTrue(result.pois.isEmpty())
        assertTrue(result.entrances.isEmpty())
    }
}