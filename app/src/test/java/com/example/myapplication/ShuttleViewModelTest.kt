package com.example.myapplication.ui.viewmodel

import com.example.myapplication.data.*
import com.example.myapplication.logic.*
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * US-2.6 / 2.7 / 2.8 — ShuttleViewModel unit tests
 *
 * UnconfinedTestDispatcher is set BEFORE ViewModel construction so that
 * viewModelScope.launch does not crash on a missing Android main looper.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ShuttleViewModelTest {

    // Set main dispatcher before any ViewModel is created
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before fun setUp() { Dispatchers.setMain(testDispatcher) }
    @After  fun tearDown() { Dispatchers.resetMain() }

    // ── Fakes ─────────────────────────────────────────────────────────────

    private class FakeAvailabilityRepo(
        private val result: ShuttleAvailability = ShuttleAvailability.Active(10)
    ) : ShuttleRepository {
        override fun getAvailability(direction: ShuttleDirection) = result
    }

    private class FakeStopFinder(
        private val result: NearestStopResult
    ) : ShuttleStopFinder {
        override fun findNearest(userLocation: LatLng, direction: ShuttleDirection) = result
    }

    // Suspends immediately — never blocks
    private class FakeDirectionsRepo : ShuttleDirectionsRepository {
        override suspend fun getRoute(
            boarding: ShuttleStop, alighting: ShuttleStop, direction: ShuttleDirection
        ) = ShuttleRouteResult.NetworkError
    }

    private fun makeVm(
        availability: ShuttleAvailability = ShuttleAvailability.Active(10),
        stopResult: NearestStopResult = NearestStopResult.Found(sgwStop)
    ) = ShuttleViewModel(
        availabilityRepo = FakeAvailabilityRepo(availability),
        stopFinder       = FakeStopFinder(stopResult),
        directionsRepo   = FakeDirectionsRepo()
    )

    private val sgwStop    = ShuttleStop("sgw_main",    "SGW Stop",    "SGW",    LatLng(45.49719, -73.57859))
    private val loyolaStop = ShuttleStop("loyola_main", "Loyola Stop", "Loyola", LatLng(45.45825, -73.63913))
    private val userLocation = LatLng(45.49720, -73.57860)

    // ── US-2.7 ────────────────────────────────────────────────────────────

    @Test fun `enableShuttleMode sets isShuttleModeActive true`() {
        val vm = makeVm()
        vm.enableShuttleMode(userLocation, "SGW")
        assertTrue(vm.isShuttleModeActive)
    }

    @Test fun `enableShuttleMode Active sets isShuttleEnabled true`() {
        val vm = makeVm(ShuttleAvailability.Active(15))
        vm.enableShuttleMode(userLocation, "SGW")
        assertTrue(vm.isShuttleEnabled)
    }

    @Test fun `WeekendOrHoliday disables shuttle with correct message`() {
        val vm = makeVm(ShuttleAvailability.WeekendOrHoliday, NearestStopResult.NoStopsAvailable)
        vm.enableShuttleMode(userLocation, "SGW")
        assertFalse(vm.isShuttleEnabled)
        assertEquals("Shuttle does not operate on weekends", vm.shuttleStatusText)
    }

    @Test fun `OutOfService disables shuttle`() {
        val vm = makeVm(ShuttleAvailability.OutOfService, NearestStopResult.NoStopsAvailable)
        vm.enableShuttleMode(userLocation, "SGW")
        assertFalse(vm.isShuttleEnabled)
    }

    @Test fun `SGW startCampus infers SGW_TO_LOYOLA direction`() {
        val vm = makeVm()
        vm.enableShuttleMode(userLocation, "SGW")
        assertEquals(ShuttleDirection.SGW_TO_LOYOLA, vm.currentDirection)
    }

    @Test fun `Loyola startCampus infers LOYOLA_TO_SGW direction`() {
        val vm = makeVm(stopResult = NearestStopResult.Found(loyolaStop))
        vm.enableShuttleMode(userLocation, "Loyola")
        assertEquals(ShuttleDirection.LOYOLA_TO_SGW, vm.currentDirection)
    }

    // ── US-2.8 ────────────────────────────────────────────────────────────

    @Test fun `Found nearest stop is stored`() {
        val vm = makeVm()
        vm.enableShuttleMode(userLocation, "SGW")
        assertEquals(sgwStop, vm.nearestStop)
    }

    @Test fun `Ambiguous result populates ambiguousStops and clears nearestStop`() {
        val candidates = listOf(sgwStop, sgwStop.copy(id = "sgw_alt"))
        val vm = makeVm(stopResult = NearestStopResult.Ambiguous(candidates))
        vm.enableShuttleMode(userLocation, "SGW")
        assertEquals(2, vm.ambiguousStops.size)
        assertNull(vm.nearestStop)
    }

    @Test fun `onUserSelectedStop resolves ambiguity`() {
        val candidates = listOf(sgwStop, sgwStop.copy(id = "sgw_alt"))
        val vm = makeVm(stopResult = NearestStopResult.Ambiguous(candidates))
        vm.enableShuttleMode(userLocation, "SGW")
        vm.onUserSelectedStop(sgwStop)
        assertEquals(sgwStop, vm.selectedStop)
        assertTrue(vm.ambiguousStops.isEmpty())
    }

    // ── disableShuttleMode ────────────────────────────────────────────────

    @Test fun `disableShuttleMode resets all state`() {
        val vm = makeVm()
        vm.enableShuttleMode(userLocation, "SGW")
        vm.disableShuttleMode()
        assertFalse(vm.isShuttleModeActive)
        assertFalse(vm.isShuttleEnabled)
        assertNull(vm.shuttleRoute)
        assertNull(vm.nearestStop)
        assertEquals("", vm.shuttleStatusText)
    }

    // ── US-2.8.4 ─────────────────────────────────────────────────────────

    @Test fun `onUserLocationUpdated ignored when shuttle inactive`() {
        val vm = makeVm()
        // Do NOT call enableShuttleMode — shuttle is inactive
        vm.onUserLocationUpdated(userLocation)
        assertNull(vm.nearestStop)
    }
}
