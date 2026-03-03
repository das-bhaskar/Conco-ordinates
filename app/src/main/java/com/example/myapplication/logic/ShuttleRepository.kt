package com.example.myapplication.logic

import com.example.myapplication.data.ShuttleAvailability
import com.example.myapplication.data.ShuttleDirection
import com.example.myapplication.data.ShuttleStopData
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.Duration

interface ShuttleRepository {
    fun getAvailability(direction: ShuttleDirection): ShuttleAvailability
}

class ShuttleRepositoryImpl : ShuttleRepository {

    override fun getAvailability(direction: ShuttleDirection): ShuttleAvailability {
        return try {
            val now     = java.time.LocalDateTime.now()
            val today   = now.dayOfWeek
            val nowTime = now.toLocalTime()

            // 1. Weekend check
            if (today == DayOfWeek.SATURDAY || today == DayOfWeek.SUNDAY) {
                return ShuttleAvailability.WeekendOrHoliday
            }

            val departuresStr = if (direction == ShuttleDirection.SGW_TO_LOYOLA)
                ShuttleStopData.SGW_DEPARTURES
            else
                ShuttleStopData.LOYOLA_DEPARTURES

            val fmt = DateTimeFormatter.ofPattern("HH:mm")
            val allDepartures = departuresStr.map { LocalTime.parse(it, fmt) }.sorted()

            if (allDepartures.isEmpty()) return ShuttleAvailability.OutOfService

            val firstDeparture = allDepartures.first()
            val lastDeparture  = allDepartures.last()

            // 2. Logic: Out of service if it's after the last bus
            if (nowTime.isAfter(lastDeparture)) {
                return ShuttleAvailability.OutOfService
            }

            // 3. Logic: Out of service if it's more than 30 minutes before the first bus
            if (nowTime.isBefore(firstDeparture)) {
                val diffToFirst = Duration.between(nowTime, firstDeparture).toMinutes()
                if (diffToFirst > 30) {
                    return ShuttleAvailability.OutOfService
                }
            }

            // 4. Find the next departure
            val nextDeparture = allDepartures.find { it.isAfter(nowTime) }

            if (nextDeparture == null) {
                // This case is technically covered by "after lastDeparture", but included for safety
                return ShuttleAvailability.OutOfService
            }

            val minutesUntil = Duration.between(nowTime, nextDeparture).toMinutes().toInt()
            ShuttleAvailability.Active(minutesUntil)

        } catch (e: Exception) {
            ShuttleAvailability.ScheduleUnavailable
        }
    }
}
