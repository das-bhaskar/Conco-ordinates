package com.example.myapplication

import com.example.myapplication.logic.InterpolatingMockRouteProvider
import com.example.myapplication.logic.SimpleMockRouteProvider
import com.google.android.gms.maps.model.LatLng
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.mock

class RouteProviderTest {


    @Test
    fun `test SimpleMockRouteProvider`() = runTest{
        val mockRouteProvider = SimpleMockRouteProvider(null)
        val answer = mockRouteProvider.getRoute(LatLng(0.0, 0.0), LatLng(0.0, 0.0), "walk")
        assert(answer == null)
    }

    @Test
    fun `test InterpolatingMockRouteProvider`() = runTest{

    }

    @Test
    fun `test getRoute interpolates correctly`() = runTest {
        val start = LatLng(0.0, 0.0)
        val end = LatLng(10.0, 20.0)
        val steps = 5u
        val provider = InterpolatingMockRouteProvider(steps)

        val route = provider.getRoute(start, end, mode = "driving")

        assertEquals(6, route?.points?.size)

        assertEquals(start, route?.points?.first())

        assertEquals(end, route?.points?.last())

        val expectedPoints = listOf(
            LatLng(0.0, 0.0),
            LatLng(2.0, 4.0),
            LatLng(4.0, 8.0),
            LatLng(6.0, 12.0),
            LatLng(8.0, 16.0),
            LatLng(10.0, 20.0)
        )

        route?.points?.forEachIndexed { index, point ->
            assertEquals(expectedPoints[index], point)
        }

        assertEquals("0 mins", route?.duration)
        assertEquals("0 km", route?.distance)
    }
}