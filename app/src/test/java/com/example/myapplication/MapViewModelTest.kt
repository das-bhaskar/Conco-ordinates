package com.example.myapplication.ui.viewmodel

import com.example.myapplication.data.Campus
import com.example.myapplication.data.CampusRepo
import com.example.myapplication.data.JsonLatLng
import com.example.myapplication.data.ShuttleRepo
import com.example.myapplication.logic.DefaultShuttleService
import com.example.myapplication.logic.MockLocationProvider
import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class MapViewModelTest {

    private lateinit var viewModel: MapViewModel
    private lateinit var mockProvider: MockLocationProvider

    @Before
    fun setup() {
        // 1. Create fake data for the test
        val testCampus = Campus(
            name = "SGW",
            center = JsonLatLng(45.497, -73.579),
            buildings = emptyList(),
            outline = emptyList()
        )

        // 2. Inject it into the Repo so it's not null anymore
        CampusRepo.setTestCampuses(listOf(testCampus))

        mockProvider = MockLocationProvider()
        // shuttleService is now required – inject the real default.
        // ShuttleRepo returns empty lists until initialized, which is fine
        // for these non-shuttle tests.
        viewModel = MapViewModel(
            locationProvider = mockProvider,
            shuttleService   = DefaultShuttleService(ShuttleRepo)
        )
    }

    @Test
    fun `onCampusSelected updates currentCampus state`() {
        // Now this will not be null!
        viewModel.onCampusSelected("SGW")
        assertEquals("SGW", viewModel.currentCampus?.name)
    }

    @Test
    fun `refreshLocation processes provider update`() {
        val testLocation = LatLng(45.497, -73.579)
        mockProvider.mockedLocation = testLocation

        viewModel.refreshLocation()

        // Assert that the current campus matches the location we just fed it
        assertEquals("SGW", viewModel.currentCampus?.name)
    }
}