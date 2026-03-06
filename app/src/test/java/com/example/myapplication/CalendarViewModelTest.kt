package com.example.myapplication.ui.viewmodel

import com.example.myapplication.data.CalendarEvent
import com.example.myapplication.logic.CalendarInfo
import com.example.myapplication.logic.FakeCalendarPreferences
import com.example.myapplication.logic.MockCalendarProvider
import com.example.myapplication.ui.models.CalendarState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [CalendarViewModel].
 *
 * Uses [MockCalendarProvider] and [FakeCalendarPreferences] — zero Android
 * framework dependencies. [StandardTestDispatcher] gives deterministic
 * coroutine execution via [advanceUntilIdle].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    // ── Test fixtures ─────────────────────────────────────────────────────────

    private val calInfo = CalendarInfo(id = "cal-1", name = "My Courses", isPrimary = true)

    private fun futureEvent(id: String = "evt-1", location: String = "H 820 SGW") = CalendarEvent(
        id          = id,
        calendarId  = "cal-1",
        title       = "SOEN 357 Lecture",
        startTimeMs = System.currentTimeMillis() + 3_600_000, // 1 hour from now
        endTimeMs   = System.currentTimeMillis() + 7_200_000,
        location    = location
    )

    private fun makeViewModel(
        calendars: List<CalendarInfo>    = listOf(calInfo),
        events:    List<CalendarEvent>   = listOf(futureEvent()),
        savedId:   String?               = null,
        savedName: String?               = null
    ): Pair<CalendarViewModel, FakeCalendarPreferences> {
        val prefs    = FakeCalendarPreferences()
        if (savedId != null && savedName != null) prefs.saveSelection(savedId, savedName)
        val provider = MockCalendarProvider(calendars = calendars, events = events)
        val vm = CalendarViewModel(
            calendarProvider    = provider,
            calendarPreferences = prefs
        )
        return vm to prefs
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test
    fun `initial state is Idle when no saved selection`() = runTest {
        val (vm, _) = makeViewModel()
        // init coroutine finds no saved prefs → stays Idle
        advanceUntilIdle()
        assertEquals(CalendarState.Idle, vm.calendarState)
    }

    @Test
    fun `initial selectedCalendarId is null when no saved selection`() = runTest {
        val (vm, _) = makeViewModel()
        advanceUntilIdle()
        assertNull(vm.selectedCalendarId)
    }

    // ── Restore on init ───────────────────────────────────────────────────────

    @Test
    fun `restores saved selection on init`() = runTest {
        val (vm, _) = makeViewModel(savedId = "cal-1", savedName = "My Courses")
        advanceUntilIdle()
        assertEquals("cal-1",      vm.selectedCalendarId)
        assertEquals("My Courses", vm.selectedCalendarName)
    }

    @Test
    fun `restore loads week events`() = runTest {
        val (vm, _) = makeViewModel(savedId = "cal-1", savedName = "My Courses")
        advanceUntilIdle()
        assertTrue(vm.weekEvents.isNotEmpty())
    }

    @Test
    fun `restore with upcoming event sets NextClassReady`() = runTest {
        val (vm, _) = makeViewModel(savedId = "cal-1", savedName = "My Courses")
        advanceUntilIdle()
        assertTrue(vm.calendarState is CalendarState.NextClassReady)
    }

    @Test
    fun `restore with no upcoming event sets NoUpcomingClass`() = runTest {
        val (vm, _) = makeViewModel(
            events    = emptyList(),
            savedId   = "cal-1",
            savedName = "My Courses"
        )
        advanceUntilIdle()
        assertTrue(vm.calendarState is CalendarState.NoUpcomingClass)
    }

    // ── loadCalendarsAndAutoSelect ────────────────────────────────────────────

    @Test
    fun `loadCalendarsAndAutoSelect sets SelectingCalendar when calendars found`() = runTest {
        val (vm, _) = makeViewModel()
        vm.loadCalendarsAndAutoSelect()
        advanceUntilIdle()
        assertTrue(vm.calendarState is CalendarState.SelectingCalendar)
    }

    @Test
    fun `loadCalendarsAndAutoSelect sets Error when no calendars`() = runTest {
        val (vm, _) = makeViewModel(calendars = emptyList())
        vm.loadCalendarsAndAutoSelect()
        advanceUntilIdle()
        assertTrue(vm.calendarState is CalendarState.Error)
    }

    // ── onCalendarSelected ────────────────────────────────────────────────────

    @Test
    fun `onCalendarSelected updates selectedCalendarId`() = runTest {
        val (vm, _) = makeViewModel()
        vm.onCalendarSelected("cal-1", "My Courses")
        advanceUntilIdle()
        assertEquals("cal-1", vm.selectedCalendarId)
    }

    @Test
    fun `onCalendarSelected persists selection to prefs`() = runTest {
        val (vm, prefs) = makeViewModel()
        vm.onCalendarSelected("cal-1", "My Courses")
        advanceUntilIdle()
        assertEquals("cal-1",      prefs.getSelectedCalendarId())
        assertEquals("My Courses", prefs.getSelectedCalendarName())
    }

    @Test
    fun `onCalendarSelected with events sets NextClassReady`() = runTest {
        val (vm, _) = makeViewModel()
        vm.onCalendarSelected("cal-1", "My Courses")
        advanceUntilIdle()
        assertTrue(vm.calendarState is CalendarState.NextClassReady)
    }

    @Test
    fun `onCalendarSelected with no events sets NoUpcomingClass`() = runTest {
        val (vm, _) = makeViewModel(events = emptyList())
        vm.onCalendarSelected("cal-1", "My Courses")
        advanceUntilIdle()
        assertTrue(vm.calendarState is CalendarState.NoUpcomingClass)
    }

    // ── clearSelection ────────────────────────────────────────────────────────

    @Test
    fun `clearSelection resets state to Idle`() = runTest {
        val (vm, _) = makeViewModel(savedId = "cal-1", savedName = "My Courses")
        advanceUntilIdle()
        vm.clearSelection()
        assertEquals(CalendarState.Idle, vm.calendarState)
    }

    @Test
    fun `clearSelection clears selectedCalendarId`() = runTest {
        val (vm, _) = makeViewModel(savedId = "cal-1", savedName = "My Courses")
        advanceUntilIdle()
        vm.clearSelection()
        assertNull(vm.selectedCalendarId)
    }

    @Test
    fun `clearSelection clears prefs`() = runTest {
        val (vm, prefs) = makeViewModel(savedId = "cal-1", savedName = "My Courses")
        advanceUntilIdle()
        vm.clearSelection()
        assertNull(prefs.getSelectedCalendarId())
    }

    @Test
    fun `clearSelection empties weekEvents`() = runTest {
        val (vm, _) = makeViewModel(savedId = "cal-1", savedName = "My Courses")
        advanceUntilIdle()
        vm.clearSelection()
        assertTrue(vm.weekEvents.isEmpty())
    }

    // ── nextUpcomingEvent ─────────────────────────────────────────────────────

    @Test
    fun `nextUpcomingEvent returns closest future event with location`() = runTest {
        val soon  = futureEvent("evt-soon", "H 820 SGW").copy(startTimeMs = System.currentTimeMillis() + 1_000)
        val later = futureEvent("evt-later", "MB S1.401 SGW").copy(startTimeMs = System.currentTimeMillis() + 7_200_000)
        val (vm, _) = makeViewModel(events = listOf(later, soon), savedId = "cal-1", savedName = "My Courses")
        advanceUntilIdle()
        assertEquals("evt-soon", vm.nextUpcomingEvent?.id)
    }

    @Test
    fun `nextUpcomingEvent is null when weekEvents is empty`() = runTest {
        val (vm, _) = makeViewModel(events = emptyList())
        advanceUntilIdle()
        assertNull(vm.nextUpcomingEvent)
    }

    @Test
    fun `nextUpcomingEvent ignores events with no location`() = runTest {
        val noLoc = futureEvent("evt-noloc", location = "")
        val (vm, _) = makeViewModel(events = listOf(noLoc), savedId = "cal-1", savedName = "My Courses")
        advanceUntilIdle()
        assertNull(vm.nextUpcomingEvent)
    }

    // ── week navigation ───────────────────────────────────────────────────────

    @Test
    fun `goToNextWeek advances currentWeekStartMs by 7 days`() = runTest {
        val (vm, _) = makeViewModel(savedId = "cal-1", savedName = "My Courses")
        advanceUntilIdle()
        val before = vm.currentWeekStartMs
        vm.goToNextWeek("cal-1")
        advanceUntilIdle()
        assertEquals(before + 7L * 24 * 60 * 60 * 1000, vm.currentWeekStartMs)
    }

    @Test
    fun `goToPreviousWeek rewinds currentWeekStartMs by 7 days`() = runTest {
        val (vm, _) = makeViewModel(savedId = "cal-1", savedName = "My Courses")
        advanceUntilIdle()
        val before = vm.currentWeekStartMs
        vm.goToPreviousWeek("cal-1")
        advanceUntilIdle()
        assertEquals(before - 7L * 24 * 60 * 60 * 1000, vm.currentWeekStartMs)
    }
}
