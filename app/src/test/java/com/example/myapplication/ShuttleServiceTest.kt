package com.example.myapplication.logic

import com.example.myapplication.data.NearestStopResult
import com.example.myapplication.data.ShuttleAvailability
import com.example.myapplication.data.ShuttleRepo
import com.example.myapplication.data.ShuttleStop
import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.Calendar

/**
 * Unit tests for [DefaultShuttleService].
 * Pure JVM – no Android emulator required.
 *
 * ShuttleRepo.setTestData() is called in @Before so tests never depend
 * on a real Context or res/raw file.
 *
 * US-2.7  Shuttle availability
 * US-2.8  Nearest stop detection
 */
class ShuttleServiceTest {

    private lateinit var service: DefaultShuttleService

    // Minimal but realistic test data mirroring shuttle_schedule.json
    private val testStops = listOf(
        ShuttleStop("sgw_main",    "SGW Stop",    "SGW",    LatLng(45.49719, -73.57859)),
        ShuttleStop("loyola_main", "Loyola Stop", "Loyola", LatLng(45.45825, -73.63913))
    )

    private val testSchedules = mapOf(
        "SGW"    to listOf("09:15", "09:45", "13:15", "13:45", "18:45"),
        "Loyola" to listOf("09:15", "10:15", "13:15", "14:15", "18:45")
    )

    @Before
    fun setUp() {
        // Inject test data so DefaultShuttleService never sees an empty schedule
        ShuttleRepo.setTestData(testStops, testSchedules)
        service = DefaultShuttleService(ShuttleRepo)
    }

    // ═════════════════════════════════════════════════════════════════════════
    // US-2.7  checkAvailability
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    fun `checkAvailability returns WeekendOrHoliday on Saturday`() {
        val result = service.checkAvailability("SGW", calendarAt(Calendar.SATURDAY, 14, 0))
        assertTrue(result is ShuttleAvailability.WeekendOrHoliday)
    }

    @Test
    fun `checkAvailability returns WeekendOrHoliday on Sunday`() {
        val result = service.checkAvailability("SGW", calendarAt(Calendar.SUNDAY, 10, 0))
        assertTrue(result is ShuttleAvailability.WeekendOrHoliday)
    }

    @Test
    fun `checkAvailability returns Active before first departure`() {
        // 08:00 Monday – first SGW departure is 09:15
        val result = service.checkAvailability("SGW", calendarAt(Calendar.MONDAY, 8, 0))
        assertTrue(result is ShuttleAvailability.Active)
    }

    @Test
    fun `checkAvailability Active contains correct minutes until next shuttle`() {
        // 09:00 Monday → next SGW departure 09:15 → 15 min away
        val result = service.checkAvailability("SGW", calendarAt(Calendar.MONDAY, 9, 0))
        assertTrue(result is ShuttleAvailability.Active)
        assertEquals(15, (result as ShuttleAvailability.Active).nextDepartureMinutes)
    }

    @Test
    fun `checkAvailability returns Active mid-day on weekday`() {
        // 13:00 Wednesday → next SGW departure 13:15 → 15 min away
        val result = service.checkAvailability("SGW", calendarAt(Calendar.WEDNESDAY, 13, 0))
        assertTrue(result is ShuttleAvailability.Active)
        assertEquals(15, (result as ShuttleAvailability.Active).nextDepartureMinutes)
    }

    @Test
    fun `checkAvailability returns OutOfService after last SGW departure`() {
        // 19:00 Friday – last SGW departure was 18:45
        val result = service.checkAvailability("SGW", calendarAt(Calendar.FRIDAY, 19, 0))
        assertTrue(result is ShuttleAvailability.OutOfService)
    }

    @Test
    fun `checkAvailability returns OutOfService after last Loyola departure`() {
        // 19:00 Thursday – last Loyola departure was 18:45
        val result = service.checkAvailability("Loyola", calendarAt(Calendar.THURSDAY, 19, 0))
        assertTrue(result is ShuttleAvailability.OutOfService)
    }

    @Test
    fun `checkAvailability returns Active for Loyola campus mid-day`() {
        // 10:00 Tuesday → next Loyola departure 10:15 → 15 min away
        val result = service.checkAvailability("Loyola", calendarAt(Calendar.TUESDAY, 10, 0))
        assertTrue(result is ShuttleAvailability.Active)
        assertEquals(15, (result as ShuttleAvailability.Active).nextDepartureMinutes)
    }

    @Test
    fun `checkAvailability returns ScheduleUnavailable for unknown campus`() {
        val result = service.checkAvailability("UNKNOWN", calendarAt(Calendar.MONDAY, 10, 0))
        assertTrue(result is ShuttleAvailability.ScheduleUnavailable)
    }

    // ═════════════════════════════════════════════════════════════════════════
    // US-2.7  statusMessage
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    fun `statusMessage contains Weekend on Saturday`() {
        val msg = service.statusMessage("SGW", calendarAt(Calendar.SATURDAY, 10, 0))
        assertTrue(msg.contains("Weekend", ignoreCase = true))
    }

    @Test
    fun `statusMessage contains next departure time when available`() {
        // 09:00 Monday → next is 09:15
        val msg = service.statusMessage("SGW", calendarAt(Calendar.MONDAY, 9, 0))
        assertTrue(msg.contains("09:15"))
    }

    @Test
    fun `statusMessage contains minutes until next departure`() {
        // 09:00 → next 09:15 → "in 15 min"
        val msg = service.statusMessage("SGW", calendarAt(Calendar.MONDAY, 9, 0))
        assertTrue(msg.contains("15 min"))
    }

    @Test
    fun `statusMessage says no more shuttles after last departure`() {
        val msg = service.statusMessage("SGW", calendarAt(Calendar.MONDAY, 19, 30))
        assertTrue(msg.contains("No more", ignoreCase = true))
    }

    // ═════════════════════════════════════════════════════════════════════════
    // US-2.8  nearestStop
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    fun `nearestStop returns LocationUnavailable when location is null`() {
        val result = service.resolveNearestStop(null)
        assertTrue(result is NearestStopResult.LocationUnavailable)
    }

    @Test
    fun `nearestStop returns Found with SGW stop for user near SGW`() {
        val result = service.resolveNearestStop(LatLng(45.4970, -73.5790))
        assertTrue(result is NearestStopResult.Found)
        assertEquals("SGW", (result as NearestStopResult.Found).stop.campus)
    }

    @Test
    fun `nearestStop returns Found with Loyola stop for user near Loyola`() {
        val result = service.resolveNearestStop(LatLng(45.4582, -73.6391))
        assertTrue(result is NearestStopResult.Found)
        assertEquals("Loyola", (result as NearestStopResult.Found).stop.campus)
    }

    @Test
    fun `nearestStop Found stop id matches injected test data`() {
        val result = service.resolveNearestStop(LatLng(45.49719, -73.57859))  // Exact SGW coords
        assertTrue(result is NearestStopResult.Found)
        assertEquals("sgw_main", (result as NearestStopResult.Found).stop.id)
    }

    @Test
    fun `nearestStop is deterministic for same input`() {
        val point = LatLng(45.4776, -73.6088)
        val first  = service.resolveNearestStop(point)
        val second = service.resolveNearestStop(point)
        val firstId  = first.idOrNull()
        val secondId = second.idOrNull()
        assertNotNull(firstId)
        assertEquals(firstId, secondId)
    }

    @Test
    fun `nearestStop returns NoStopsAvailable when repo has no stops`() {
        ShuttleRepo.setTestData(emptyList(), emptyMap())
        val result = service.resolveNearestStop(LatLng(45.49, -73.57))
        assertTrue(result is NearestStopResult.NoStopsAvailable)
        // Restore for other tests
        ShuttleRepo.setTestData(testStops, testSchedules)
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Helpers
    // ═════════════════════════════════════════════════════════════════════════

    private fun calendarAt(dayOfWeek: Int, hour: Int, minute: Int): Calendar =
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, dayOfWeek)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

    private fun NearestStopResult.idOrNull(): String? = when (this) {
        is NearestStopResult.Found     -> stop.id
        is NearestStopResult.Ambiguous -> candidates.firstOrNull()?.id
        else                           -> null
    }
}
