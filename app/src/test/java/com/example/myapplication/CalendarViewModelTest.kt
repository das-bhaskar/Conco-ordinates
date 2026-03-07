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
 *
 * After the PR #282 refactor:
 *  - [CalendarState.NextClassReady] and [CalendarState.NoUpcomingClass] are
 *    removed. "Next class" state is exposed as [CalendarViewModel.nextUpcomingEvent]
 *    (a derived property), so tests assert on that property instead.
 *  - [FakeCalendarPreferences.saveSelection] now takes [CalendarInfo].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    // ── Test fixtures ─────────────────────────────────────────────────────────

    private val calInfo = CalendarInfo(id = "cal-1", summary = "My Courses")

    private fun futureEvent(id: String = "evt-1", location: String = "H 820 SGW") = CalendarEvent(
        id          = id,
        calendarId  = "cal-1",
        title       = "SOEN 357 Lecture",
        startTimeMs = System.currentTimeMillis() + 3_600_000, // 1 hour from now
        endTimeMs   = System.currentTimeMillis() + 7_200_000,
        location    = location
    )

    private fun makeViewModel(
        calendars: List<CalendarInfo>  = listOf(calInfo),
        events:    List<CalendarEvent> = listOf(futureEvent()),
        savedId:   String?             = null,
        savedName: String?             = null
    ): Pair<CalendarViewModel, FakeCalendarPreferences> {
        val prefs = FakeCalendarPreferences()
        if (savedId != null && savedName != null) {
            prefs.saveSelection(CalendarInfo(id = savedId, summary = savedName))
        }
        val provider = MockCalendarProvider(calendars = calendars, events = events)
        val vm = CalendarViewModel(
            calendarProvider    = provider,
            calendarPreferences = prefs
        )
        return vm to prefs
    }

    @Before
    fun setup() { Dispatchers.setMain(testDispatcher) }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test
    fun `initial state is Idle when no saved selection`() = runTest {
        val (vm, _) = makeViewModel()
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
    fun `restore with upcoming event exposes nextUpcomingEvent`() = runTest {
        val (vm, _) = makeViewModel(savedId = "cal-1", savedName = "My Courses")
        advanceUntilIdle()
        // CalendarState.NextClassReady is removed — next class is a derived property
        assertNotNull(vm.nextUpcomingEvent)
        assertEquals(CalendarState.Idle, vm.calendarState)
    }

    @Test
    fun `restore with no upcoming event has null nextUpcomingEvent`() = runTest {
        val (vm, _) = makeViewModel(
            events    = emptyList(),
            savedId   = "cal-1",
            savedName = "My Courses"
        )
        advanceUntilIdle()
        // CalendarState.NoUpcomingClass is removed — check derived property instead
        assertNull(vm.nextUpcomingEvent)
        assertEquals(CalendarState.Idle, vm.calendarState)
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
    fun `onCalendarSelected with events exposes nextUpcomingEvent`() = runTest {
        val (vm, _) = makeViewModel()
        vm.onCalendarSelected("cal-1", "My Courses")
        advanceUntilIdle()
        assertNotNull(vm.nextUpcomingEvent)
        assertEquals(CalendarState.Idle, vm.calendarState)
    }

    @Test
    fun `onCalendarSelected with no events has null nextUpcomingEvent`() = runTest {
        val (vm, _) = makeViewModel(events = emptyList())
        vm.onCalendarSelected("cal-1", "My Courses")
        advanceUntilIdle()
        assertNull(vm.nextUpcomingEvent)
        assertEquals(CalendarState.Idle, vm.calendarState)
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
        val soon  = futureEvent("evt-soon",  "H 820 SGW").copy(startTimeMs = System.currentTimeMillis() + 1_000)
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

    // ── isNextClassUrgent ─────────────────────────────────────────────────────

    @Test
    fun `isNextClassUrgent is false when no upcoming event`() = runTest {
        val (vm, _) = makeViewModel(events = emptyList())
        advanceUntilIdle()
        assertFalse(vm.isNextClassUrgent)
    }

    @Test
    fun `isNextClassUrgent is false when event is more than 15 min away`() = runTest {
        val farEvent = futureEvent().copy(startTimeMs = System.currentTimeMillis() + 20 * 60_000)
        val (vm, _) = makeViewModel(events = listOf(farEvent), savedId = "cal-1", savedName = "My Courses")
        advanceUntilIdle()
        assertFalse(vm.isNextClassUrgent)
    }

    @Test
    fun `isNextClassUrgent is true when event is within 15 min`() = runTest {
        val urgentEvent = futureEvent().copy(startTimeMs = System.currentTimeMillis() + 5 * 60_000)
        val (vm, _) = makeViewModel(events = listOf(urgentEvent), savedId = "cal-1", savedName = "My Courses")
        advanceUntilIdle()
        assertTrue(vm.isNextClassUrgent)
    }

    // ── week navigation ───────────────────────────────────────────────────────

    @Test
    fun `goToNextWeek advances currentWeekStartMs by 7 days`() = runTest {
        val (vm, _) = makeViewModel(savedId = "cal-1", savedName = "My Courses")
        advanceUntilIdle()
        val beforeZdt = java.time.ZonedDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(vm.currentWeekStartMs),
            java.time.ZoneId.systemDefault()
        )
        vm.goToNextWeek("cal-1")
        advanceUntilIdle()
        val afterZdt = java.time.ZonedDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(vm.currentWeekStartMs),
            java.time.ZoneId.systemDefault()
        )
        // ZonedDateTime comparison survives DST transitions — raw ms arithmetic does not.
        // goToNextWeek uses plusWeeks(1) internally, so the result is exactly 7 calendar
        // days later regardless of any DST boundary crossed.
        assertEquals(beforeZdt.plusWeeks(1), afterZdt)
    }

    @Test
    fun `goToPreviousWeek rewinds currentWeekStartMs by 7 days`() = runTest {
        val (vm, _) = makeViewModel(savedId = "cal-1", savedName = "My Courses")
        advanceUntilIdle()
        val beforeZdt = java.time.ZonedDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(vm.currentWeekStartMs),
            java.time.ZoneId.systemDefault()
        )
        vm.goToPreviousWeek("cal-1")
        advanceUntilIdle()
        val afterZdt = java.time.ZonedDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(vm.currentWeekStartMs),
            java.time.ZoneId.systemDefault()
        )
        assertEquals(beforeZdt.minusWeeks(1), afterZdt)
    }
}
