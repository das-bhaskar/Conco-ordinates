package com.example.myapplication.ui.viewmodel

import com.example.myapplication.data.*
import com.example.myapplication.logic.*
import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * US-2.6 / 2.7 / 2.8 — ShuttleViewModel unit tests
 * Uses fakes injected through the constructor.
 */
class ShuttleViewModelTest {

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

    private class FakeDirectionsRepo(
        private val result: ShuttleRouteResult = ShuttleRouteResult.NetworkError
    ) : ShuttleDirectionsRepository {
        override suspend fun getRoute(boarding: ShuttleStop, alighting: ShuttleStop, direction: ShuttleDirection) = result
    }

    private val sgwStop = ShuttleStop("sgw_main", "SGW Stop", "SGW", LatLng(45.49719, -73.57859))
    private val loyolaStop = ShuttleStop("loyola_main", "Loyola Stop", "Loyola", LatLng(45.45825, -73.63913))
    private val userLocation = LatLng(45.49720, -73.57860)

    // ── US-2.7 Tests ──────────────────────────────────────────────────────

    @Test fun `enableShuttleMode sets isShuttleModeActive true`() {
        val vm = ShuttleViewModel(
            availabilityRepo = FakeAvailabilityRepo(ShuttleAvailability.Active(5)),
            stopFinder       = FakeStopFinder(NearestStopResult.Found(sgwStop)),
            directionsRepo   = FakeDirectionsRepo()
        )
        vm.enableShuttleMode(userLocation, "SGW")
        assertTrue(vm.isShuttleModeActive)
    }

    @Test fun `enableShuttleMode with Active availability sets isShuttleEnabled true`() {
        val vm = ShuttleViewModel(
            availabilityRepo = FakeAvailabilityRepo(ShuttleAvailability.Active(15)),
            stopFinder       = FakeStopFinder(NearestStopResult.Found(sgwStop)),
            directionsRepo   = FakeDirectionsRepo()
        )
        vm.enableShuttleMode(userLocation, "SGW")
        assertTrue(vm.isShuttleEnabled)
    }

    @Test fun `enableShuttleMode on weekend disables shuttle`() {
        val vm = ShuttleViewModel(
            availabilityRepo = FakeAvailabilityRepo(ShuttleAvailability.WeekendOrHoliday),
            stopFinder       = FakeStopFinder(NearestStopResult.NoStopsAvailable),
            directionsRepo   = FakeDirectionsRepo()
        )
        vm.enableShuttleMode(userLocation, "SGW")
        assertFalse(vm.isShuttleEnabled)
        assertEquals("Shuttle does not operate on weekends", vm.shuttleStatusText)
    }

    @Test fun `enableShuttleMode OutOfService disables shuttle`() {
        val vm = ShuttleViewModel(
            availabilityRepo = FakeAvailabilityRepo(ShuttleAvailability.OutOfService),
            stopFinder       = FakeStopFinder(NearestStopResult.NoStopsAvailable),
            directionsRepo   = FakeDirectionsRepo()
        )
        vm.enableShuttleMode(userLocation, "SGW")
        assertFalse(vm.isShuttleEnabled)
    }

    // ── US-2.7 AC3: Direction inference ───────────────────────────────────

    @Test fun `SGW startCampus sets SGW_TO_LOYOLA direction`() {
        val vm = ShuttleViewModel(
            availabilityRepo = FakeAvailabilityRepo(),
            stopFinder       = FakeStopFinder(NearestStopResult.Found(sgwStop)),
            directionsRepo   = FakeDirectionsRepo()
        )
        vm.enableShuttleMode(userLocation, "SGW")
        assertEquals(ShuttleDirection.SGW_TO_LOYOLA, vm.currentDirection)
    }

    @Test fun `Loyola startCampus sets LOYOLA_TO_SGW direction`() {
        val vm = ShuttleViewModel(
            availabilityRepo = FakeAvailabilityRepo(),
            stopFinder       = FakeStopFinder(NearestStopResult.Found(loyolaStop)),
            directionsRepo   = FakeDirectionsRepo()
        )
        vm.enableShuttleMode(userLocation, "Loyola")
        assertEquals(ShuttleDirection.LOYOLA_TO_SGW, vm.currentDirection)
    }

    // ── US-2.8 Tests ──────────────────────────────────────────────────────

    @Test fun `Found nearest stop sets nearestStop`() {
        val vm = ShuttleViewModel(
            availabilityRepo = FakeAvailabilityRepo(),
            stopFinder       = FakeStopFinder(NearestStopResult.Found(sgwStop)),
            directionsRepo   = FakeDirectionsRepo()
        )
        vm.enableShuttleMode(userLocation, "SGW")
        assertEquals(sgwStop, vm.nearestStop)
    }

    @Test fun `Ambiguous stops sets ambiguousStops list`() {
        val candidates = listOf(sgwStop, sgwStop.copy(id = "sgw_alt"))
        val vm = ShuttleViewModel(
            availabilityRepo = FakeAvailabilityRepo(),
            stopFinder       = FakeStopFinder(NearestStopResult.Ambiguous(candidates)),
            directionsRepo   = FakeDirectionsRepo()
        )
        vm.enableShuttleMode(userLocation, "SGW")
        assertEquals(2, vm.ambiguousStops.size)
        assertNull(vm.nearestStop)
    }

    @Test fun `onUserSelectedStop resolves ambiguity`() {
        val candidates = listOf(sgwStop, sgwStop.copy(id = "sgw_alt"))
        val vm = ShuttleViewModel(
            availabilityRepo = FakeAvailabilityRepo(),
            stopFinder       = FakeStopFinder(NearestStopResult.Ambiguous(candidates)),
            directionsRepo   = FakeDirectionsRepo()
        )
        vm.enableShuttleMode(userLocation, "SGW")
        vm.onUserSelectedStop(sgwStop)
        assertEquals(sgwStop, vm.selectedStop)
        assertTrue(vm.ambiguousStops.isEmpty())
    }

    // ── disableShuttleMode ────────────────────────────────────────────────

    @Test fun `disableShuttleMode clears all state`() {
        val vm = ShuttleViewModel(
            availabilityRepo = FakeAvailabilityRepo(),
            stopFinder       = FakeStopFinder(NearestStopResult.Found(sgwStop)),
            directionsRepo   = FakeDirectionsRepo()
        )
        vm.enableShuttleMode(userLocation, "SGW")
        vm.disableShuttleMode()

        assertFalse(vm.isShuttleModeActive)
        assertFalse(vm.isShuttleEnabled)
        assertNull(vm.shuttleRoute)
        assertNull(vm.nearestStop)
        assertEquals("", vm.shuttleStatusText)
    }

    // ── US-2.8.4: location update ─────────────────────────────────────────

    @Test fun `onUserLocationUpdated does nothing when shuttle inactive`() {
        val vm = ShuttleViewModel(
            availabilityRepo = FakeAvailabilityRepo(),
            stopFinder       = FakeStopFinder(NearestStopResult.Found(sgwStop)),
            directionsRepo   = FakeDirectionsRepo()
        )
        // Not enabled — location update should be ignored
        vm.onUserLocationUpdated(userLocation)
        assertNull(vm.nearestStop) // nearestStop never set
    }
}
