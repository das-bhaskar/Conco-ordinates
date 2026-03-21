package com.example.myapplication.ui.viewmodel

import com.example.myapplication.data.CalendarEvent
import com.example.myapplication.logic.CalendarInfo
import com.example.myapplication.logic.FakeCalendarPreferences
import com.example.myapplication.logic.MockCalendarProvider
import com.example.myapplication.logic.currentWeekMonday
import com.example.myapplication.ui.models.CalendarState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
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

    private val testDispatcher = StandardTestDispatcher()    // ── Test fixtures ─────────────────────────────────────────────────────────

    private val calInfo = CalendarInfo(id = "cal-1", summary = "My Courses")

    private fun futureEvent(id: String = "evt-1", location: String = "H 820 SGW") = CalendarEvent(
        id          = id,
        calendarId  = "cal-1",
        title       = "SOEN 357 Lecture",
        // Use the same time source as the ViewModel's currentWeekStartMs
        startTimeMs = currentWeekMonday() + 3_600_000,
        endTimeMs   = currentWeekMonday() + 7_200_000,
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
            calendarPreferences = prefs,
            dispatcher = testDispatcher,
            locationResolver    = com.example.myapplication.logic.LocationResolver(
                buildingNames = { code ->
                    mapOf(
                        "H"  to "Henry F. Hall Building",
                        "MB" to "John Molson Building",
                        "EV" to "Engineering & Visual Arts",
                        "HC" to "Hingston Hall"
                    )[code]
                }
            )
        )
        return vm to prefs
    }

    @Before
    fun setup() { Dispatchers.setMain(testDispatcher) }

    @After
    fun tearDown() {
        testDispatcher.cancelChildren()
        Dispatchers.resetMain()
    }

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test(timeout = 5000)

    fun `initial state is Idle when no saved selection`() = runTest {
        val (vm, _) = makeViewModel()
runCurrent() 
    
    // 2. Advance time by 61 seconds to ensure the tickerFlow triggers
    testDispatcher.scheduler.advanceTimeBy(61_000)
    
    // 3. Run the code that was scheduled by that time jump
    runCurrent()

        assertEquals(CalendarState.Idle, vm.calendarState)

        testDispatcher.cancelChildren()
    }

    @Test(timeout = 5000)

    fun `initial selectedCalendarId is null when no saved selection`() = runTest {
        val (vm, _) = makeViewModel()
runCurrent() 
    
    // 2. Advance time by 61 seconds to ensure the tickerFlow triggers
    testDispatcher.scheduler.advanceTimeBy(61_000)
    
    // 3. Run the code that was scheduled by that time jump
    runCurrent()

        assertNull(vm.selectedCalendarId)

        testDispatcher.cancelChildren()
    }

    // ── Restore on init ───────────────────────────────────────────────────────

    @Test(timeout = 5000)

    fun `restores saved selection on init`() = runTest {
        val (vm, _) = makeViewModel(savedId = "cal-1", savedName = "My Courses")
runCurrent() 
    
    // 2. Advance time by 61 seconds to ensure the tickerFlow triggers
    testDispatcher.scheduler.advanceTimeBy(61_000)
    
    // 3. Run the code that was scheduled by that time jump
    runCurrent()

        assertEquals("cal-1",      vm.selectedCalendarId)
        assertEquals("My Courses", vm.selectedCalendarName)

        testDispatcher.cancelChildren()

    }





    @Test(timeout = 5000)

    fun `restore with no upcoming event has null nextUpcomingEvent`() = runTest {
        val (vm, _) = makeViewModel(
            events    = emptyList(),
            savedId   = "cal-1",
            savedName = "My Courses"
        )
runCurrent() 
    
    // 2. Advance time by 61 seconds to ensure the tickerFlow triggers
    testDispatcher.scheduler.advanceTimeBy(61_000)
    
    // 3. Run the code that was scheduled by that time jump
    runCurrent()

        // CalendarState.NoUpcomingClass is removed — check derived property instead

        assertNull(vm.nextUpcomingEvent)
        assertEquals(CalendarState.Idle, vm.calendarState)

        testDispatcher.cancelChildren()

    }

    // ── loadCalendarsAndAutoSelect ────────────────────────────────────────────

    @Test(timeout = 5000)

    fun `loadCalendarsAndAutoSelect sets SelectingCalendar when calendars found`() = runTest {
        val (vm, _) = makeViewModel()
        vm.loadCalendarsAndAutoSelect()
runCurrent() 
    
    // 2. Advance time by 61 seconds to ensure the tickerFlow triggers
    testDispatcher.scheduler.advanceTimeBy(61_000)
    
    // 3. Run the code that was scheduled by that time jump
    runCurrent()

        assertTrue(vm.calendarState is CalendarState.SelectingCalendar)

        testDispatcher.cancelChildren()

    }

    @Test(timeout = 5000)

    fun `loadCalendarsAndAutoSelect sets Error when no calendars`() = runTest {
        val (vm, _) = makeViewModel(calendars = emptyList())
        vm.loadCalendarsAndAutoSelect()
runCurrent() 
    
    // 2. Advance time by 61 seconds to ensure the tickerFlow triggers
    testDispatcher.scheduler.advanceTimeBy(61_000)
    
    // 3. Run the code that was scheduled by that time jump
    runCurrent()

        assertTrue(vm.calendarState is CalendarState.Error)

        testDispatcher.cancelChildren()

    }

    // ── onCalendarSelected ────────────────────────────────────────────────────

    @Test(timeout = 5000)

    fun `onCalendarSelected updates selectedCalendarId`() = runTest {
        val (vm, _) = makeViewModel()
        vm.onCalendarSelected("cal-1", "My Courses")
runCurrent() 
    
    // 2. Advance time by 61 seconds to ensure the tickerFlow triggers
    testDispatcher.scheduler.advanceTimeBy(61_000)
    
    // 3. Run the code that was scheduled by that time jump
    runCurrent()

        assertEquals("cal-1", vm.selectedCalendarId)

        testDispatcher.cancelChildren()

    }

    @Test(timeout = 5000)

    fun `onCalendarSelected persists selection to prefs`() = runTest {
        val (vm, prefs) = makeViewModel()
        vm.onCalendarSelected("cal-1", "My Courses")
runCurrent() 
    
    // 2. Advance time by 61 seconds to ensure the tickerFlow triggers
    testDispatcher.scheduler.advanceTimeBy(61_000)
    
    // 3. Run the code that was scheduled by that time jump
    runCurrent()

        assertEquals("cal-1",      prefs.getSelectedCalendarId())
        assertEquals("My Courses", prefs.getSelectedCalendarName())

        testDispatcher.cancelChildren()

    }



    @Test(timeout = 5000)

    fun `onCalendarSelected with no events has null nextUpcomingEvent`() = runTest {
        val (vm, _) = makeViewModel(events = emptyList())
        vm.onCalendarSelected("cal-1", "My Courses")
runCurrent() 
    
    // 2. Advance time by 61 seconds to ensure the tickerFlow triggers
    testDispatcher.scheduler.advanceTimeBy(61_000)
    
    // 3. Run the code that was scheduled by that time jump
    runCurrent()

        assertNull(vm.nextUpcomingEvent)
        assertEquals(CalendarState.Idle, vm.calendarState)


        testDispatcher.cancelChildren()
    }

    // ── clearSelection ────────────────────────────────────────────────────────

    @Test(timeout = 5000)

    fun `clearSelection resets state to Idle`() = runTest {
        val (vm, _) = makeViewModel(savedId = "cal-1", savedName = "My Courses")
runCurrent() 
    
    // 2. Advance time by 61 seconds to ensure the tickerFlow triggers
    testDispatcher.scheduler.advanceTimeBy(61_000)
    
    // 3. Run the code that was scheduled by that time jump
    runCurrent()

        vm.clearSelection()
        assertEquals(CalendarState.Idle, vm.calendarState)

        testDispatcher.cancelChildren()

    }

    @Test(timeout = 5000)

    fun `clearSelection clears selectedCalendarId`() = runTest {
        val (vm, _) = makeViewModel(savedId = "cal-1", savedName = "My Courses")
runCurrent() 
    
    // 2. Advance time by 61 seconds to ensure the tickerFlow triggers
    testDispatcher.scheduler.advanceTimeBy(61_000)
    
    // 3. Run the code that was scheduled by that time jump
    runCurrent()

        vm.clearSelection()
        assertNull(vm.selectedCalendarId)

        testDispatcher.cancelChildren()

    }

    @Test(timeout = 5000)

    fun `clearSelection clears prefs`() = runTest {
        val (vm, prefs) = makeViewModel(savedId = "cal-1", savedName = "My Courses")
runCurrent() 
    
    // 2. Advance time by 61 seconds to ensure the tickerFlow triggers
    testDispatcher.scheduler.advanceTimeBy(61_000)
    
    // 3. Run the code that was scheduled by that time jump
    runCurrent()

        vm.clearSelection()
        assertNull(prefs.getSelectedCalendarId())

        testDispatcher.cancelChildren()

    }

    @Test(timeout = 5000)

    fun `clearSelection empties weekEvents`() = runTest {
        val (vm, _) = makeViewModel(savedId = "cal-1", savedName = "My Courses")
runCurrent() 
    
    // 2. Advance time by 61 seconds to ensure the tickerFlow triggers
    testDispatcher.scheduler.advanceTimeBy(61_000)
    
    // 3. Run the code that was scheduled by that time jump
    runCurrent()

        vm.clearSelection()
        assertTrue(vm.weekEvents.isEmpty())

        testDispatcher.cancelChildren()

    }

    // ── nextUpcomingEvent ─────────────────────────────────────────────────────



    @Test(timeout = 5000)

    fun `nextUpcomingEvent is null when weekEvents is empty`() = runTest {
        val (vm, _) = makeViewModel(events = emptyList())
runCurrent() 
    
    // 2. Advance time by 61 seconds to ensure the tickerFlow triggers
    testDispatcher.scheduler.advanceTimeBy(61_000)
    
    // 3. Run the code that was scheduled by that time jump
    runCurrent()

        assertNull(vm.nextUpcomingEvent)

        testDispatcher.cancelChildren()

    }

    @Test(timeout = 5000)

    fun `nextUpcomingEvent ignores events with no location`() = runTest {
        val noLoc = futureEvent("evt-noloc", location = "")
        val (vm, _) = makeViewModel(events = listOf(noLoc), savedId = "cal-1", savedName = "My Courses")
runCurrent() 
    
    // 2. Advance time by 61 seconds to ensure the tickerFlow triggers
    testDispatcher.scheduler.advanceTimeBy(61_000)
    
    // 3. Run the code that was scheduled by that time jump
    runCurrent()

        assertNull(vm.nextUpcomingEvent)
        testDispatcher.cancelChildren()


    }

    // ── isNextClassUrgent ─────────────────────────────────────────────────────

    @Test(timeout = 5000)

    fun `isNextClassUrgent is false when no upcoming event`() = runTest {
        val (vm, _) = makeViewModel(events = emptyList())
runCurrent() 
    
    // 2. Advance time by 61 seconds to ensure the tickerFlow triggers
    testDispatcher.scheduler.advanceTimeBy(61_000)
    
    // 3. Run the code that was scheduled by that time jump
    runCurrent()

        assertFalse(vm.isNextClassUrgent)

        testDispatcher.cancelChildren()

    }

    @Test(timeout = 5000)
    fun `restore loads week events`() = runTest {
        // 1. Setup
        val (vm, _) = makeViewModel(savedId = "cal-1", savedName = "My Courses")

        // 2. Advance the virtual clock to allow the restore coroutine AND
        // the ticker's first refresh to execute.
        testDispatcher.scheduler.advanceUntilIdle()

        // 3. Assert
        assertTrue("Events should not be empty after restore", vm.weekEvents.isNotEmpty())

        testDispatcher.cancelChildren()
    }

    @Test(timeout = 5000)
    fun `isNextClassUrgent is false when event is more than 15 min away`() = runTest {
        // Manually set a time relative to the virtual clock
        val now = testDispatcher.scheduler.currentTime
        val farEvent = futureEvent().copy(startTimeMs = now + 20 * 60_000)

        val (vm, _) = makeViewModel(events = listOf(farEvent), savedId = "cal-1", savedName = "My Courses")

        advanceUntilIdle()

        assertFalse(vm.isNextClassUrgent)
        testDispatcher.cancelChildren()
    }
    // ── week navigation ───────────────────────────────────────────────────────

    @Test(timeout = 5000)

    fun `goToNextWeek advances currentWeekStartMs by 7 days`() = runTest {
        val (vm, _) = makeViewModel(savedId = "cal-1", savedName = "My Courses")
runCurrent() 
    
    // 2. Advance time by 61 seconds to ensure the tickerFlow triggers
    testDispatcher.scheduler.advanceTimeBy(61_000)
    
    // 3. Run the code that was scheduled by that time jump
    runCurrent()

        val beforeZdt = java.time.ZonedDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(vm.currentWeekStartMs),
            java.time.ZoneId.systemDefault()
        )
        vm.goToNextWeek("cal-1")
runCurrent() 
    
    // 2. Advance time by 61 seconds to ensure the tickerFlow triggers
    testDispatcher.scheduler.advanceTimeBy(61_000)
    
    // 3. Run the code that was scheduled by that time jump
    runCurrent()

        val afterZdt = java.time.ZonedDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(vm.currentWeekStartMs),
            java.time.ZoneId.systemDefault()
        )
        // ZonedDateTime comparison survives DST transitions — raw ms arithmetic does not.
        // goToNextWeek uses plusWeeks(1) internally, so the result is exactly 7 calendar
        // days later regardless of any DST boundary crossed.
        assertEquals(beforeZdt.plusWeeks(1), afterZdt)

        testDispatcher.cancelChildren()

    }

    @Test(timeout = 5000)

    fun `goToPreviousWeek rewinds currentWeekStartMs by 7 days`() = runTest {
        val (vm, _) = makeViewModel(savedId = "cal-1", savedName = "My Courses")
runCurrent() 
    
    // 2. Advance time by 61 seconds to ensure the tickerFlow triggers
    testDispatcher.scheduler.advanceTimeBy(61_000)
    
    // 3. Run the code that was scheduled by that time jump
    runCurrent()

        val beforeZdt = java.time.ZonedDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(vm.currentWeekStartMs),
            java.time.ZoneId.systemDefault()
        )
        vm.goToPreviousWeek("cal-1")
runCurrent() 
    
    // 2. Advance time by 61 seconds to ensure the tickerFlow triggers
    testDispatcher.scheduler.advanceTimeBy(61_000)
    
    // 3. Run the code that was scheduled by that time jump
    runCurrent()

        val afterZdt = java.time.ZonedDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(vm.currentWeekStartMs),
            java.time.ZoneId.systemDefault()
        )
        assertEquals(beforeZdt.minusWeeks(1), afterZdt)

        testDispatcher.cancelChildren()

    }

//    @Test(timeout = 5000)
//    fun `restore with upcoming event exposes nextUpcomingEvent`() = runTest {
//        val (vm, _) = makeViewModel(savedId = "cal-1", savedName = "My Courses")
//
//        // This forces the loadWeekEvents job AND the ticker delay to complete
//        testDispatcher.scheduler.advanceUntilIdle()
//
//        assertNotNull(vm.nextUpcomingEvent)
//        assertEquals(CalendarState.Idle, vm.calendarState)
//        testDispatcher.cancelChildren()
//    }
//
//    @Test(timeout = 5000)
//    fun `onCalendarSelected with events exposes nextUpcomingEvent`() = runTest {
//        val (vm, _) = makeViewModel()
//        vm.onCalendarSelected("cal-1", "My Courses")
//
//        testDispatcher.scheduler.advanceUntilIdle()
//
//        assertNotNull(vm.nextUpcomingEvent)
//        assertEquals(CalendarState.Idle, vm.calendarState)
//        testDispatcher.cancelChildren()
//    }
//
//    @Test(timeout = 5000)
//    fun `nextUpcomingEvent returns closest future event with location`() = runTest {
//        val soon  = futureEvent("evt-soon",  "H 820 SGW").copy(startTimeMs = System.currentTimeMillis() + 1_000)
//        val later = futureEvent("evt-later", "MB S1.401 SGW").copy(startTimeMs = System.currentTimeMillis() + 7_200_000)
//        val (vm, _) = makeViewModel(events = listOf(later, soon), savedId = "cal-1", savedName = "My Courses")
//
//        testDispatcher.scheduler.advanceUntilIdle()
//
//        assertEquals("evt-soon", vm.nextUpcomingEvent?.id)
//        testDispatcher.cancelChildren()
//    }
//
//    @Test(timeout = 5000)
//    fun `isNextClassUrgent is true when event is within 15 min`() = runTest {
//        val urgentEvent = futureEvent().copy(startTimeMs = System.currentTimeMillis() + 5 * 60_000)
//        val (vm, _) = makeViewModel(events = listOf(urgentEvent), savedId = "cal-1", savedName = "My Courses")
//
//        testDispatcher.scheduler.advanceUntilIdle()
//
//        assertTrue(vm.isNextClassUrgent)
//        testDispatcher.cancelChildren()
//    }
}
