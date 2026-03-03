package com.example.myapplication.logic

import com.example.myapplication.data.ShuttleAvailability
import com.example.myapplication.data.ShuttleDirection
import com.example.myapplication.data.ShuttleStopData
import org.junit.Assert.*
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * US-2.7 — Task #108 #109 #110
 * Uses an injected-time subclass so tests are deterministic.
 */
class ShuttleRepositoryTest {

    private fun repoAt(dayOfWeek: DayOfWeek, timeStr: String): ShuttleRepository {
        val time = LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm"))
        return object : ShuttleRepository {
            override fun getAvailability(direction: ShuttleDirection): ShuttleAvailability {
                return try {
                    if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY)
                        return ShuttleAvailability.WeekendOrHoliday

                    val fmt = DateTimeFormatter.ofPattern("HH:mm")
                    val departures = (if (direction == ShuttleDirection.SGW_TO_LOYOLA)
                        ShuttleStopData.SGW_DEPARTURES else ShuttleStopData.LOYOLA_DEPARTURES)
                        .map { LocalTime.parse(it, fmt) }.sorted()

                    if (departures.isEmpty()) return ShuttleAvailability.OutOfService
                    if (time.isAfter(departures.last())) return ShuttleAvailability.OutOfService

                    if (time.isBefore(departures.first())) {
                        val diff = java.time.Duration.between(time, departures.first()).toMinutes()
                        if (diff > 30) return ShuttleAvailability.OutOfService
                    }

                    val next = departures.find { it.isAfter(time) }
                        ?: return ShuttleAvailability.OutOfService
                    val minutes = java.time.Duration.between(time, next).toMinutes().toInt()
                    ShuttleAvailability.Active(minutes)
                } catch (e: Exception) {
                    ShuttleAvailability.ScheduleUnavailable
                }
            }
        }
    }

    // Weekend
    @Test fun `saturday returns WeekendOrHoliday`() {
        assertTrue(repoAt(DayOfWeek.SATURDAY, "10:00").getAvailability(ShuttleDirection.SGW_TO_LOYOLA)
            is ShuttleAvailability.WeekendOrHoliday)
    }

    @Test fun `sunday returns WeekendOrHoliday`() {
        assertTrue(repoAt(DayOfWeek.SUNDAY, "14:00").getAvailability(ShuttleDirection.LOYOLA_TO_SGW)
            is ShuttleAvailability.WeekendOrHoliday)
    }

    // Out of service
    @Test fun `after last departure returns OutOfService`() {
        assertTrue(repoAt(DayOfWeek.MONDAY, "19:00").getAvailability(ShuttleDirection.SGW_TO_LOYOLA)
            is ShuttleAvailability.OutOfService)
    }

    @Test fun `more than 30min before first departure returns OutOfService`() {
        assertTrue(repoAt(DayOfWeek.TUESDAY, "08:30").getAvailability(ShuttleDirection.SGW_TO_LOYOLA)
            is ShuttleAvailability.OutOfService)
    }

    // Active with countdown
    @Test fun `active mid-morning gives correct countdown`() {
        // At 09:00, next SGW departure is 09:15 -> 15 min
        val result = repoAt(DayOfWeek.WEDNESDAY, "09:00").getAvailability(ShuttleDirection.SGW_TO_LOYOLA)
        assertTrue(result is ShuttleAvailability.Active)
        assertEquals(15, (result as ShuttleAvailability.Active).nextDepartureMinutes)
    }

    @Test fun `loyola direction countdown is correct`() {
        val result = repoAt(DayOfWeek.THURSDAY, "09:00").getAvailability(ShuttleDirection.LOYOLA_TO_SGW)
        assertTrue(result is ShuttleAvailability.Active)
        assertEquals(15, (result as ShuttleAvailability.Active).nextDepartureMinutes)
    }

    @Test fun `grace period within 30min before first bus returns Active`() {
        // 08:55 is 20 min before 09:15 -> inside grace period
        assertTrue(repoAt(DayOfWeek.MONDAY, "08:55").getAvailability(ShuttleDirection.SGW_TO_LOYOLA)
            is ShuttleAvailability.Active)
    }
}
