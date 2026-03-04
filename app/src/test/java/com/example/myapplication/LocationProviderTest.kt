package com.example.myapplication.logic

import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Task
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import android.location.Location
import org.mockito.kotlin.whenever
import com.google.android.gms.tasks.OnSuccessListener
import org.mockito.kotlin.*

class LocationProviderTest {

    @Test
    fun `test mock provider returns correct data`() {
        val mock = MockLocationProvider()
        val testLoc = LatLng(45.0, -73.0)
        mock.mockedLocation = testLoc

        mock.getUserLocation { location ->
            assertEquals(testLoc, location)
        }
    }

    private lateinit var client: FusedLocationProviderClient
    private lateinit var locationProvider: TrueLocationProvider
    private lateinit var mockTask: Task<Location>

    @Before
    fun setup() {
        client = mock()
        mockTask = mock()
        locationProvider = TrueLocationProvider(client)

        whenever(client.lastLocation).thenReturn(mockTask)
    }

    @Test
    fun `test getUserLocation with available location return LatLng of location`() {
        val mockLocation: Location = mock()
        whenever(mockLocation.latitude).thenReturn(37.7749)
        whenever(mockLocation.longitude).thenReturn(-122.4194)

        whenever(mockTask.addOnSuccessListener(any<OnSuccessListener<Location>>())).thenAnswer { invocation ->
            val listener = invocation.getArgument<OnSuccessListener<Location>>(0)
            listener.onSuccess(mockLocation)
            mockTask
        }

        val callback: (LatLng?) -> Unit = mock()
        locationProvider.getUserLocation(callback)

        verify(callback).invoke(LatLng(37.7749, -122.4194))
    }

    @Test
    fun `test getUserLocation with null return null`() {
        whenever(mockTask.addOnSuccessListener(any<OnSuccessListener<Location>>())).thenAnswer { invocation ->
            val listener = invocation.getArgument<OnSuccessListener<Location>>(0)
            listener.onSuccess(null)
            mockTask
        }

        val callback: (LatLng?) -> Unit = mock()
        locationProvider.getUserLocation(callback)

        verify(callback).invoke(null)
    }

}