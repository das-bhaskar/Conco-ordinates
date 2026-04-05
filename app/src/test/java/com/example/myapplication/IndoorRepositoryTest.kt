package com.example.myapplication.data.indoor

import android.content.Context
import android.content.res.Resources
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream

class IndoorRepositoryTest {

    private val context = mockk<Context>()
    private val resources = mockk<Resources>()
    private val parser = mockk<IndoorJsonParser>()

    private lateinit var repository: IndoorRepository

    @Before
    fun setUp() {
        every { context.resources } returns resources
        every { context.packageName } returns "com.example.myapplication"

        repository = IndoorRepository(context, parser)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `getFloor should return null when raw resource does not exist`() = runTest {
        every {
            resources.getIdentifier(
                "indoor_h_floor1",
                "raw",
                "com.example.myapplication"
            )
        } returns 0

        val result = repository.getFloor("H", 1)

        assertNull(result)
        verify(exactly = 1) {
            resources.getIdentifier("indoor_h_floor1", "raw", "com.example.myapplication")
        }
        verify(exactly = 0) { resources.openRawResource(any()) }
        verify(exactly = 0) { parser.parse(any()) }
    }

    @Test
    fun `getFloor should load and parse resource when it exists`() = runTest {
        val expected = floor(building = "H", floor = 1)

        every {
            resources.getIdentifier(
                "indoor_h_floor1",
                "raw",
                "com.example.myapplication"
            )
        } returns 123

        every { resources.openRawResource(123) } returns jsonStream(
            """
            {
              "building": "H",
              "floor": 1
            }
            """.trimIndent()
        )

        every { parser.parse(any()) } returns expected

        val result = repository.getFloor("H", 1)

        assertSame(expected, result)
        verify(exactly = 1) {
            resources.getIdentifier("indoor_h_floor1", "raw", "com.example.myapplication")
        }
        verify(exactly = 1) { resources.openRawResource(123) }
        verify(exactly = 1) { parser.parse(any()) }
    }

    @Test
    fun `getFloor should cache loaded floor and avoid reloading`() = runTest {
        val expected = floor(building = "H", floor = 1)

        every {
            resources.getIdentifier(
                "indoor_h_floor1",
                "raw",
                "com.example.myapplication"
            )
        } returns 123

        every { resources.openRawResource(123) } returns jsonStream(
            """
            {
              "building": "H",
              "floor": 1
            }
            """.trimIndent()
        )

        every { parser.parse(any()) } returns expected

        val first = repository.getFloor("H", 1)
        val second = repository.getFloor("h", 1)

        assertSame(expected, first)
        assertSame(expected, second)

        verify(exactly = 1) {
            resources.getIdentifier("indoor_h_floor1", "raw", "com.example.myapplication")
        }
        verify(exactly = 1) { resources.openRawResource(123) }
        verify(exactly = 1) { parser.parse(any()) }
    }

    @Test
    fun `getFloor should use n prefix for negative floors`() = runTest {
        var capturedName = ""

        every {
            resources.getIdentifier(any(), "raw", "com.example.myapplication")
        } answers {
            capturedName = firstArg()
            0
        }

        val result = repository.getFloor("MB", -2)

        assertNull(result)
        assertEquals("indoor_mb_floorn2", capturedName)
    }

    @Test
    fun `getFloor should return null when parser throws`() = runTest {
        every {
            resources.getIdentifier(
                "indoor_cc_floor2",
                "raw",
                "com.example.myapplication"
            )
        } returns 999

        every { resources.openRawResource(999) } returns jsonStream(
            """
            {
              "building": "CC",
              "floor": 2
            }
            """.trimIndent()
        )

        every { parser.parse(any()) } throws RuntimeException("parse failed")

        val result = repository.getFloor("CC", 2)

        assertNull(result)
        verify(exactly = 1) {
            resources.getIdentifier("indoor_cc_floor2", "raw", "com.example.myapplication")
        }
        verify(exactly = 1) { resources.openRawResource(999) }
        verify(exactly = 1) { parser.parse(any()) }
    }

    @Test
    fun `getFloor should return null when json is invalid`() = runTest {
        every {
            resources.getIdentifier(
                "indoor_ev_floor1",
                "raw",
                "com.example.myapplication"
            )
        } returns 77

        every { resources.openRawResource(77) } returns jsonStream("not valid json")

        val result = repository.getFloor("EV", 1)

        assertNull(result)
        verify(exactly = 1) {
            resources.getIdentifier("indoor_ev_floor1", "raw", "com.example.myapplication")
        }
        verify(exactly = 1) { resources.openRawResource(77) }
        verify(exactly = 0) { parser.parse(any()) }
    }

    private fun jsonStream(text: String) =
        ByteArrayInputStream(text.toByteArray())

    private fun floor(building: String, floor: Int) = IndoorFloor(
        building = building,
        floor = floor,
        rooms = emptyList(),
        corridors = emptyList(),
        nodes = emptyList(),
        edges = emptyList(),
        pois = emptyList(),
        entrances = emptyList()
    )
}