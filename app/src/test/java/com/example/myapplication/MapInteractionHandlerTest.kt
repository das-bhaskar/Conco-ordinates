

package com.example.myapplication.logic

import android.content.Context
import com.example.myapplication.data.Building
import com.example.myapplication.data.Campus
import com.example.myapplication.ui.viewmodel.MapViewModel
import com.google.android.gms.maps.model.LatLng
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

class MapInteractionHandlerTest {

    private lateinit var mockViewModel: MapViewModel
    private lateinit var mockContext: Context
    private lateinit var mockCampus: Campus
    private lateinit var mockBuilding: Building

    @Before
    fun setup() {
        mockViewModel = mock()
        mockContext = mock()
        mockCampus = mock()
        mockBuilding = mock()

        // Setup common mock behavior
        whenever(mockBuilding.name).thenReturn("Hall Building")
        whenever(mockBuilding.getCenter()).thenReturn(LatLng(45.497, -73.579))
        whenever(mockBuilding.getGoogleOutline()).thenReturn(emptyList())

        whenever(mockCampus.buildings).thenReturn(listOf(mockBuilding))
        whenever(mockViewModel.currentCampus).thenReturn(mockCampus)

        // Disable Firebase for tests
        com.example.myapplication.telemetry.CrashReporter.isTesting = true
    }

    @Test
    fun `processClick calls handleMapTap with building when building is found`() {
        // 1. Arrange: Define a click point
        val clickPoint = LatLng(45.497, -73.579)
        val buildingName = "Hall Building"

        // 2. Setup Mock Building with a valid "Box" outline around the click point
        whenever(mockBuilding.name).thenReturn(buildingName)
        // This outline creates a small square around the clickPoint (45.497, -73.579)
        whenever(mockBuilding.getGoogleOutline()).thenReturn(listOf(
            LatLng(45.496, -73.580),
            LatLng(45.498, -73.580),
            LatLng(45.498, -73.578),
            LatLng(45.496, -73.578)
        ))

        // Ensure the campus contains this building
        whenever(mockCampus.buildings).thenReturn(listOf(mockBuilding))

        // 3. Act
        MapInteractionHandler.processClick(clickPoint, mockViewModel, mockContext)

        // 4. Assert
        // We expect the building to be passed now because the point is "inside" the square
        verify(mockViewModel, atLeastOnce()).handleMapTap(eq(mockBuilding), anyOrNull())
    }

    @Test
    fun `processClick calls handleMapTap with null when no building is found`() {
        whenever(mockCampus.buildings).thenReturn(emptyList())

        // Use a point that clearly won't hit anything
        val farAwayPoint = LatLng(0.0, 0.0)

        // 2. Act
        MapInteractionHandler.processClick(farAwayPoint, mockViewModel, mockContext)

        // 3. Assert: Verify handleMapTap was called with null
        // We use eq(null) or just null to be explicit
        verify(mockViewModel).handleMapTap(null, null)
    }

    @Test
    fun `handleSearchSelection updates viewmodel with building data`() {
        MapInteractionHandler.handleSearchSelection(mockBuilding, mockViewModel, mockContext)

        // Verify handleMapTap was triggered
        verify(mockViewModel, atLeastOnce()).handleMapTap(eq(mockBuilding), anyOrNull())
    }
}