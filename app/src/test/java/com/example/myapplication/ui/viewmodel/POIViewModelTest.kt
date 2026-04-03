package com.example.myapplication.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.myapplication.data.poi.POI
import com.example.myapplication.data.poi.POICategory
import com.example.myapplication.data.poi.POIException
import com.example.myapplication.data.poi.POIRepository
import com.example.myapplication.ui.models.POIUiState
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class POIViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakePOIRepository
    private lateinit var viewModel: POIViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakePOIRepository()
        viewModel = POIViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `openPOIPanel enters loading while waiting for location`() {
        viewModel.openPOIPanel()

        assertEquals(POIUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `onLocationUpdated fetches all POIs when panel is loading`() = runTest {
        repository.result = listOf(TEST_POI)

        viewModel.openPOIPanel()
        viewModel.onLocationUpdated(TEST_ORIGIN)
        advanceUntilIdle()

        val state = viewModel.uiState.value as POIUiState.Browse
        assertEquals(TEST_ORIGIN, repository.lastOrigin)
        assertEquals(POICategory.ALL, repository.lastCategory)
        assertEquals(listOf(TEST_POI), state.pois)
    }

    @Test
    fun `onCategorySelected refetches with selected category and current radius`() = runTest {
        repository.result = listOf(TEST_POI)

        viewModel.openPOIPanel()
        viewModel.onLocationUpdated(TEST_ORIGIN)
        advanceUntilIdle()

        val secondPoi = TEST_POI.copy(category = POICategory.CAFE)
        repository.result = listOf(secondPoi)

        viewModel.onCategorySelected(POICategory.CAFE)
        advanceUntilIdle()

        val state = viewModel.uiState.value as POIUiState.Browse
        assertEquals(POICategory.CAFE, repository.lastCategory)
        assertEquals(POIRepository.DEFAULT_RADIUS, repository.lastRadius)
        assertEquals(secondPoi, state.pois.single())
    }

    @Test
    fun `onPOISelected and onPOIDismissed update selected item in success state`() = runTest {
        repository.result = listOf(TEST_POI)

        viewModel.openPOIPanel()
        viewModel.onLocationUpdated(TEST_ORIGIN)
        advanceUntilIdle()

        viewModel.onPOISelected(TEST_POI)
        assertEquals(TEST_POI, (viewModel.uiState.value as POIUiState.Selection).selectedPOI)

        viewModel.onPOIDismissed()
        val browseState = viewModel.uiState.value as POIUiState.Browse
        assertEquals(listOf(TEST_POI), browseState.pois)
    }

    @Test
    fun `fetchPOIs exposes empty state when repository returns no results`() = runTest {
        repository.result = emptyList()

        viewModel.openPOIPanel()
        viewModel.onLocationUpdated(TEST_ORIGIN)
        advanceUntilIdle()

        assertEquals(POIUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun `fetchPOIs exposes error state when repository throws POIException`() = runTest {
        repository.error = POIException("boom")

        viewModel.openPOIPanel()
        viewModel.onLocationUpdated(TEST_ORIGIN)
        advanceUntilIdle()

        assertEquals(POIUiState.Error("boom"), viewModel.uiState.value)
    }

    @Test
    fun `factory creates POIViewModel from injected repository`() {
        val created = POIViewModel.Factory(repository).create(POIViewModel::class.java)

        assertTrue(created is ViewModel)
        assertTrue(created is POIViewModel)
    }

    private class FakePOIRepository : POIRepository {
        var result: List<POI> = emptyList()
        var error: POIException? = null
        var lastOrigin: LatLng? = null
        var lastRadius: Int? = null
        var lastCategory: POICategory? = null

        override suspend fun getNearbyPOIs(
            origin: LatLng,
            radiusMeters: Int,
            category: POICategory
        ): List<POI> {
            lastOrigin = origin
            lastRadius = radiusMeters
            lastCategory = category
            error?.let { throw it }
            return result
        }
    }

    companion object {
        private val TEST_ORIGIN = LatLng(45.497, -73.579)
        private val TEST_POI = POI(
            placeId = "poi-1",
            name = "Cafe",
            address = "1455 Test",
            category = POICategory.ALL,
            latLng = TEST_ORIGIN,
            distanceMeters = 120
        )
    }
}
