package com.example.myapplication.logic

import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class LocationProviderTest {

    private lateinit var mockClient: FusedLocationProviderClient
    private lateinit var mockTask: Task<Location>
    private lateinit var locationProvider: TrueLocationProvider

    @Before
    fun setup() {
        mockClient = mock()
        mockTask = mock()
        locationProvider = TrueLocationProvider(mockClient)
    }

    @Test
    fun `getUserLocation returns LatLng when successful`() {
        // 1. Arrange: Create a fake Android Location object
        val fakeLocation = mock(Location::class.java)
        whenever(fakeLocation.latitude).thenReturn(45.497)
        whenever(fakeLocation.longitude).thenReturn(-73.579)

        // 2. Mock the Task behavior
        whenever(mockClient.lastLocation).thenReturn(mockTask)

        // This simulates the "addOnSuccessListener" callback being triggered immediately
        whenever(mockTask.addOnSuccessListener(any())).thenAnswer { invocation ->
            val listener = invocation.arguments[0] as OnSuccessListener<Location>
            listener.onSuccess(fakeLocation)
            mockTask
        }

        // 3. Act & Assert
        locationProvider.getUserLocation { result ->
            assertNotNull(result)
            assertEquals(45.497, result?.latitude ?: 0.0, 0.001)
            assertEquals(-73.579, result?.longitude ?: 0.0, 0.001)
        }
    }



    @Test
    fun `getUserLocation returns null when location is null`() {
        whenever(mockClient.lastLocation).thenReturn(mockTask)

        whenever(mockTask.addOnSuccessListener(any())).thenAnswer { invocation ->
            val listener = invocation.arguments[0] as OnSuccessListener<Location>
            listener.onSuccess(null) // Simulate no location found
            mockTask
        }

        locationProvider.getUserLocation { result ->
            assertNull(result)
        }
    }

}