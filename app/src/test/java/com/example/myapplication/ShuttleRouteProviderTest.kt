package com.example.myapplication.logic
import com.example.myapplication.data.ShuttleStop
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

class ShuttleRouteProviderTest {

    private lateinit var mockShuttleService: ShuttleService
    private lateinit var mockGoogleRouteProvider: RouteProvider
    private lateinit var shuttleRouteProvider: ShuttleRouteProvider

    // Sample coordinates for testing
    private val sgwStop = LatLng(45.497, -73.579)
    private val loyolaStop = LatLng(45.458, -73.639)
    private val userStart = LatLng(45.498, -73.580)
    private val userEnd = LatLng(45.459, -73.640)

    @Before
    fun setup() {
        mockShuttleService = mock()
        mockGoogleRouteProvider = mock()
        shuttleRouteProvider = ShuttleRouteProvider(mockShuttleService, mockGoogleRouteProvider)
    }

    @Test
    fun `getRoute returns null if nearest boarding stop cannot be resolved`() = runTest {
        // Arrange
        whenever(mockShuttleService.resolveNearestStop(userStart)).thenReturn(null)

        // Act
        val result = shuttleRouteProvider.getRoute(userStart, userEnd, "shuttle")

        // Assert
        assertNull(result)
    }

    @Test
    fun `getRoute delegates to Google and overrides duration and distance`() = runTest {
        // 1. Arrange: Define the coordinates
        val userStart = LatLng(45.498, -73.580)
        val userEnd = LatLng(45.459, -73.640)
        val sgwStopCoords = LatLng(45.497, -73.579)
        val loyolaStopCoords = LatLng(45.458, -73.639)

        // 2. Create the mock return objects with ALL 4 required parameters
        val board = ShuttleStop(
            id = "stop_1",
            name = "SGW",
            campus = "SGW",
            location = sgwStopCoords
        )
        val alight = ShuttleStop(
            id = "stop_2",
            name = "Loyola",
            campus = "Loyola",
            location = loyolaStopCoords
        )

        // Mock ShuttleService to return board first, then alight
        whenever(mockShuttleService.resolveNearestStop(anyOrNull())).thenReturn(board, alight)

        // 3. Mock Google return (Google results should be overwritten by Shuttle logic)
        val googleRoute = RouteData(
            points = listOf(sgwStopCoords, loyolaStopCoords),
            duration = "15 mins",
            distance = "7.0 km"
        )
        whenever(mockGoogleRouteProvider.getRoute(any(), any(), eq("drive")))
            .thenReturn(googleRoute)

        // 4. Act
        val result = shuttleRouteProvider.getRoute(userStart, userEnd, "shuttle")

        // 5. Assert
        assertNotNull(result)

        // Check that Google was called with the STOP coordinates, not the USER click coordinates
        verify(mockGoogleRouteProvider).getRoute(sgwStopCoords, loyolaStopCoords, "drive")

        // Verify the "Shuttle Override" logic (Constants defined in ShuttleRouteProvider)
        assertEquals("20 min", result?.duration)
        assertEquals("11.0 km", result?.distance)
        assertEquals(googleRoute.points, result?.points)
    }

    @Test
    fun `getRoute provides fallback straight line if Google provider fails`() = runTest {
        // 1. Arrange: Coordinates
        val userStart = LatLng(45.498, -73.580)
        val userEnd = LatLng(45.459, -73.640)
        val sgwCoords = LatLng(45.497, -73.579)
        val loyolaCoords = LatLng(45.458, -73.639)

        // 2. Create correct ShuttleStop objects matching your data class
        val board = ShuttleStop(
            id = "1",
            name = "SGW",
            campus = "SGW",
            location = sgwCoords
        )
        val alight = ShuttleStop(
            id = "2",
            name = "LOY",
            campus = "Loyola",
            location = loyolaCoords
        )

        // Sequential mock: first call returns board, second returns alight
        whenever(mockShuttleService.resolveNearestStop(anyOrNull())).thenReturn(board, alight)

        // Mock Google failure
        whenever(mockGoogleRouteProvider.getRoute(any(), any(), any())).thenReturn(null)

        // 3. Act
        val result = shuttleRouteProvider.getRoute(userStart, userEnd, "shuttle")

        // 4. Assert
        assertNotNull(result)

        // Polyline check: Should be exactly 2 points (the stops)
        assertEquals(2, result?.points?.size)
        assertEquals(sgwCoords, result?.points?.first())
        assertEquals(loyolaCoords, result?.points?.last())

        // Fixed constants check
        assertEquals("20 min", result?.duration)
        assertEquals("11.0 km", result?.distance)
    }
}