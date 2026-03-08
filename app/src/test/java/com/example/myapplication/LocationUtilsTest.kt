package com.example.myapplication.logic
import org.junit.Assert.fail
import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Tasks
import android.location.Location
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.*

class LocationUtilsTest {

    private val mockClient: FusedLocationProviderClient = mock()
    private val mockLauncher: ActivityResultLauncher<String> = mock()
    private val mockContext: Context = mock()

    @Test
    fun `handleRecenter calls onShowSettings when permission missing and rationale needed`() {
        var settingsCalled = false

        handleRecenter(
            client = mockClient,
            hasPermission = false,
            shouldShowRationale = true,
            launcher = mockLauncher,
            context = mockContext,
            onShowSettings = { settingsCalled = true },
            onLocationFound = {}
        )

        assertTrue("Should call onShowSettings", settingsCalled)
        verifyNoInteractions(mockLauncher)
    }

    @Test
    fun `handleRecenter launches permission request when permission missing and no rationale`() {
        handleRecenter(
            client = mockClient,
            hasPermission = false,
            shouldShowRationale = false,
            launcher = mockLauncher,
            context = mockContext,
            onShowSettings = {},
            onLocationFound = {}
        )

        verify(mockLauncher).launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
    }

    @Test
    fun `handleRecenter fetches location and calls onLocationFound when permission granted`() {
        // 1. Mock a successful Location object
        val mockLocation = mock<android.location.Location>().apply {
            whenever(latitude).thenReturn(45.4972)
            whenever(longitude).thenReturn(-73.5790)
        }

        // 2. Mock the Task
        val mockTask: com.google.android.gms.tasks.Task<android.location.Location> = mock()
        whenever(mockClient.lastLocation).thenReturn(mockTask)

        // 3. FORCE the success listener to execute immediately when added
        whenever(mockTask.addOnSuccessListener(any())).thenAnswer {
            val listener = it.arguments[0] as com.google.android.gms.tasks.OnSuccessListener<android.location.Location>
            listener.onSuccess(mockLocation) // Manually trigger the success logic
            mockTask
        }

        var foundLatLng: LatLng? = null

        handleRecenter(
            client = mockClient,
            hasPermission = true,
            shouldShowRationale = false,
            launcher = mockLauncher,
            context = mockContext,
            onShowSettings = {},
            onLocationFound = { foundLatLng = it }
        )

        // 4. Now this will pass because the listener was forced to run
        assertNotNull("foundLatLng should have been populated by the callback", foundLatLng)
        assertEquals(45.4972, foundLatLng?.latitude!!, 0.0001)
        assertEquals(-73.5790, foundLatLng?.longitude!!, 0.0001)
    }


}