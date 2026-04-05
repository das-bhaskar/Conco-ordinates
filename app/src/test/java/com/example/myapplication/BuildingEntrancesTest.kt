package com.example.myapplication.data.indoor

import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildingEntrancesTest {

    @Test
    fun `forBuilding should be case insensitive`() {
        val entrance = BuildingEntrance(
            nodeId = "n1",
            label = "Main Entrance",
            gps = LatLng(45.4971, -73.5788),
            floor = 2
        )

        val sut = BuildingEntrances(
            mapOf("CC" to listOf(entrance))
        )

        val resultLower = sut.forBuilding("cc")
        val resultMixed = sut.forBuilding("Cc")
        val resultUpper = sut.forBuilding("CC")

        assertEquals(1, resultLower.size)
        assertEquals(1, resultMixed.size)
        assertEquals(1, resultUpper.size)
        assertEquals("n1", resultLower.first().nodeId)
        assertEquals("Main Entrance", resultLower.first().label)
    }

    @Test
    fun `forBuilding should return empty list when building does not exist`() {
        val sut = BuildingEntrances()

        val result = sut.forBuilding("XYZ")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `nearest should return closest entrance for a building`() {
        val farEntrance = BuildingEntrance(
            nodeId = "far",
            label = "Far Entrance",
            gps = LatLng(45.5010, -73.5700),
            floor = 1
        )
        val nearEntrance = BuildingEntrance(
            nodeId = "near",
            label = "Near Entrance",
            gps = LatLng(45.4975, -73.5790),
            floor = 2
        )

        val sut = BuildingEntrances(
            mapOf("H" to listOf(farEntrance, nearEntrance))
        )

        val userLocation = LatLng(45.4974, -73.5791)

        val result = sut.nearest("h", userLocation)

        assertEquals("near", result?.nodeId)
        assertEquals("Near Entrance", result?.label)
        assertEquals(2, result?.floor)
    }

    @Test
    fun `nearest should return null when building has no entrances`() {
        val sut = BuildingEntrances()

        val result = sut.nearest("CC", LatLng(45.497, -73.579))

        assertNull(result)
    }

    @Test
    fun `parseJson should parse valid json and uppercase building codes`() {
        val sut = BuildingEntrances()

        val json = """
            {
              "h": [
                {
                  "nodeId": "h1",
                  "label": "Hall Main",
                  "lat": 45.4971,
                  "lng": -73.5788,
                  "floor": 2
                },
                {
                  "nodeId": "h2",
                  "label": "Hall Side",
                  "lat": 45.4972,
                  "lng": -73.5789
                }
              ],
              "cc": [
                {
                  "nodeId": "cc1",
                  "label": "CC Front",
                  "lat": 45.4950,
                  "lng": -73.5770,
                  "floor": 1
                }
              ]
            }
        """.trimIndent()

        val result = sut.parseJson(json)

        assertEquals(setOf("H", "CC"), result.keys)

        val hallEntrances = result["H"]!!
        assertEquals(2, hallEntrances.size)

        assertEquals("h1", hallEntrances[0].nodeId)
        assertEquals("Hall Main", hallEntrances[0].label)
        assertEquals(45.4971, hallEntrances[0].gps.latitude, 0.000001)
        assertEquals(-73.5788, hallEntrances[0].gps.longitude, 0.000001)
        assertEquals(2, hallEntrances[0].floor)

        assertEquals("h2", hallEntrances[1].nodeId)
        assertEquals("Hall Side", hallEntrances[1].label)
        assertEquals(45.4972, hallEntrances[1].gps.latitude, 0.000001)
        assertEquals(-73.5789, hallEntrances[1].gps.longitude, 0.000001)
        assertEquals(1, hallEntrances[1].floor) // default floor
    }

    @Test
    fun `parseJson should use default values for missing fields`() {
        val sut = BuildingEntrances()

        val json = """
            {
              "EV": [
                {
                }
              ]
            }
        """.trimIndent()

        val result = sut.parseJson(json)

        val entrances = result["EV"]!!
        assertEquals(1, entrances.size)

        val entrance = entrances.first()
        assertEquals("", entrance.nodeId)
        assertEquals("", entrance.label)
        assertEquals(0.0, entrance.gps.latitude, 0.000001)
        assertEquals(0.0, entrance.gps.longitude, 0.000001)
        assertEquals(1, entrance.floor)
    }

    @Test
    fun `parseJson should ignore blank building codes and non array values`() {
        val sut = BuildingEntrances()

        val json = """
            {
              "": [
                {
                  "nodeId": "bad",
                  "label": "Should be ignored",
                  "lat": 1.0,
                  "lng": 2.0
                }
              ],
              "H": "not_an_array",
              "MB": [
                {
                  "nodeId": "mb1",
                  "label": "MB Entrance",
                  "lat": 45.1,
                  "lng": -73.1
                }
              ]
            }
        """.trimIndent()

        val result = sut.parseJson(json)

        assertEquals(setOf("MB"), result.keys)
        assertEquals(1, result["MB"]!!.size)
        assertEquals("mb1", result["MB"]!!.first().nodeId)
    }
}