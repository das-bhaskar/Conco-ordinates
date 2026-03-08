package com.example.myapplication

import com.example.myapplication.logic.GoogleRouteProvider
import com.example.myapplication.logic.RouteData
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GoogleRouteProviderTest {
    private val provider = GoogleRouteProvider(BuildConfig.MAPS_API_KEY)

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test getRoute check impossible travel on foot`() = runTest {
        val routeData = provider.getRoute(LatLng(0.0, 0.0), LatLng(10.0, 10.0), "walking")
        assertEquals(null, routeData)
    }

    @Test
    fun `test getRoute check impossible travel on transit`() = runTest {
        val routeData =
            provider.getRoute(LatLng(7.0002375, 0.7895958), LatLng(6.9975220, 0.7919240), "transit")
        assertEquals(null, routeData)
    }

    @Test
    fun `test getRoute check impossible travel on car`() = runTest {
        val routeData = provider.getRoute(LatLng(0.0, 0.0), LatLng(10.0, 10.0), "car")
        assertEquals(null, routeData)
    }

    @Test
    fun `test getRoute check same coordinates for transit`() = runTest {
        val routeData = provider.getRoute(
            LatLng(45.4973357596697, -73.57894993830904),
            LatLng(45.4973357596697, -73.57894993830904),
            "transit"
        )
        assertEquals(null, routeData)
    }

    @Test
    fun `test getRoute check bad Key return null routeData`() = runTest {
    val badProvider = GoogleRouteProvider("0")
        val routeData = badProvider.getRoute(
            LatLng(45.4973357596697, -73.57894993830904),
            LatLng(45.4973357596697, -73.57894993830904),
            "walks"
        )
        assertEquals(null, routeData)
    }


}