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
    fun `test getRoute check random entry in mode`() = runTest {
        val routeData = provider.getRoute(
            LatLng(45.4973357596697, -73.57894993830904),
            LatLng(45.4973357596697, -73.57894993830904),
            "Tyrannosaurus"
        )
        var testListPoints = ArrayList<LatLng>()
        testListPoints.add(LatLng(45.497640000000004, -73.57866))
        assertEquals(RouteData(testListPoints, "1 min", "1 m"), routeData)
    }

    @Test
    fun `test getRoute check same coordinates for foot`() = runTest {
        val routeData = provider.getRoute(
            LatLng(45.4973357596697, -73.57894993830904),
            LatLng(45.4973357596697, -73.57894993830904),
            "walks"
        )
        var testListPoints = ArrayList<LatLng>()
        testListPoints.add(LatLng(45.497640000000004, -73.57866))
        assertEquals(RouteData(testListPoints, "1 min", "1 m"), routeData)
    }

    @Test
    fun `test getRoute check same coordinates for car`() = runTest {
        val routeData = provider.getRoute(
            LatLng(45.4973357596697, -73.57894993830904),
            LatLng(45.4973357596697, -73.57894993830904),
            "drive"
        )
        var testListPoints = ArrayList<LatLng>()
        testListPoints.add(LatLng(45.49767000000001, -73.57863))
        assertEquals(RouteData(testListPoints, "1 min", "1 m"), routeData)
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
    fun `test getRoute check normal route for walk`() = runTest {
        val routeData = provider.getRoute(
            LatLng(45.4973357596697, -73.57894993830904),
            LatLng(45.494220940920705, -73.57817776994307),
            "walking"
        )
        var testListPoints = ArrayList<LatLng>()
        testListPoints.addAll(
            arrayListOf(
                LatLng(45.497640000000004, -73.57866),
                LatLng(45.49754, -73.57845),
                LatLng(45.49736, -73.57812000000001),
                LatLng(45.49678, -73.57689),
                LatLng(45.496550000000006, -73.57645000000001),
                LatLng(45.49588000000001, -73.5771),
                LatLng(45.495020000000004, -73.57799),
                LatLng(45.4945, -73.57858),
                LatLng(45.49443, -73.57846),
                LatLng(45.49441, -73.57849)
            )
        )
        assertEquals(RouteData(testListPoints, "7 mins", "0.5 km"), routeData)
    }

    @Test
    fun `test getRoute check normal route for car`() = runTest {
        val routeData = provider.getRoute(
            LatLng(45.4973357596697, -73.57894993830904),
            LatLng(45.4954272442362, -73.57918751985744),
            "drive"
        )
        var testListPoints = ArrayList<LatLng>()
        testListPoints.addAll(
            arrayListOf(
                LatLng(45.49767000000001, -73.57863),
                LatLng(45.498090000000005, -73.57949),
                LatLng(45.49824, -73.57979),
                LatLng(45.49835, -73.57999000000001),
                LatLng(45.49821, -73.58013000000001),
                LatLng(45.49761, -73.58073),
                LatLng(45.49734, -73.58016),
                LatLng(45.496950000000005, -73.57934),
                LatLng(45.496680000000005, -73.57876),
                LatLng(45.49656, -73.57887000000001),
                LatLng(45.49645, -73.57897000000001),
                LatLng(45.49626000000001, -73.57915000000001),
                LatLng(45.49606000000001, -73.57926),
                LatLng(45.495870000000004, -73.5793),
                LatLng(45.49573, -73.57932000000001),
                LatLng(45.49569, -73.57926),
                LatLng(45.49562, -73.57935),
                LatLng(45.495560000000005, -73.57942)
            )
        )
        assertEquals(RouteData(testListPoints, "3 mins", "0.6 km"), routeData)
    }

    @Test
    fun `test getRoute check normal route for transit`() = runTest {
        val routeData = provider.getRoute(
            LatLng(45.494806334497454, -73.58038794001561),
            LatLng(45.48938088567872, -73.58410430230106),
            "transit"
        )
        var testListPoints = ArrayList<LatLng>()
        testListPoints.addAll(
            arrayListOf(
                LatLng(45.49479, -73.58039000000001),
                LatLng(45.49474, -73.58029),
                LatLng(45.49024000000001, -73.58561),
                LatLng(45.49007, -73.58582000000001),
                LatLng(45.48903000000001, -73.58504),
                LatLng(45.48937, -73.58411000000001)
            )
        )
        assertEquals(RouteData(testListPoints, "1 min", "0.9 km"), routeData)
    }

    @Test
    fun `test getRoute check route small enough that transit use walking`() = runTest {
        val routeData = provider.getRoute(
            LatLng(45.4973357596697, -73.57894993830904),
            LatLng(45.494220940920705, -73.57817776994307),
            "transit"
        )
        var testListPoints = ArrayList<LatLng>()
        testListPoints.addAll(
            arrayListOf(
                LatLng(45.497640000000004, -73.57866),
                LatLng(45.49754, -73.57845),
                LatLng(45.49736, -73.57812000000001),
                LatLng(45.49678, -73.57689),
                LatLng(45.496550000000006, -73.57645000000001),
                LatLng(45.49588000000001, -73.5771),
                LatLng(45.495020000000004, -73.57799),
                LatLng(45.4945, -73.57858),
                LatLng(45.49443, -73.57846),
                LatLng(45.49441, -73.57849)
            )
        )
        assertEquals(RouteData(testListPoints, "7 mins", "0.5 km"), routeData)
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