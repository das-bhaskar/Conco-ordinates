package com.example.myapplication.logic

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.ActivityResultLauncher
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.*

class LocationUtilsTest {

    private val mockClient: FusedLocationProviderClient = mock()
    private val mockContext: Context = ContextWrapper(null)
    private val fakeLauncher = RecordingLauncher()

    @Test
    fun `handleRecenter calls onShowSettings when permission missing and rationale needed`() {
        var settingsCalled = false

        handleRecenter(
            client = mockClient,
            hasPermission = false,
            shouldShowRationale = true,
            launcher = fakeLauncher,
            context = mockContext,
            onShowSettings = { settingsCalled = true },
            onLocationFound = {}
        )

        assertTrue("Should call onShowSettings", settingsCalled)
        assertEquals(null, fakeLauncher.lastValue)
    }

    @Test
    fun `handleRecenter launches permission request when permission missing and no rationale`() {
        handleRecenter(
            client = mockClient,
            hasPermission = false,
            shouldShowRationale = false,
            launcher = fakeLauncher,
            context = mockContext,
            onShowSettings = {},
            onLocationFound = {}
        )

        assertEquals(android.Manifest.permission.ACCESS_FINE_LOCATION, fakeLauncher.lastValue)
    }

    @Test
    fun `haversineDistanceMeters returns zero for identical coordinates`() {
        val origin = LatLng(45.4972, -73.5790)

        assertEquals(0, haversineDistanceMeters(origin, origin))
    }

    @Test
    fun `formatDistance renders meters and kilometers`() {
        assertEquals("250 m", formatDistance(250))
        assertEquals("1.3 km", formatDistance(1250))
    }

    private class RecordingLauncher : ActivityResultLauncher<String>() {
        var lastValue: String? = null

        override val contract: ActivityResultContract<String, *>
            get() = object : ActivityResultContract<String, Unit>() {
                override fun createIntent(context: Context, input: String) =
                    android.content.Intent()

                override fun parseResult(resultCode: Int, intent: android.content.Intent?) = Unit
            }

        override fun launch(input: String, options: androidx.core.app.ActivityOptionsCompat?) {
            lastValue = input
        }

        override fun unregister() = Unit
    }

}
