package com.example.myapplication.logic

import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Centralised date/time formatters (PR review).
 *
 * Previously SimpleDateFormat instances were created inline inside Composables,
 * which means they were recreated on every recomposition and could not be
 * changed to respect a user-defined or system-wide locale preference without
 * modifying multiple UI files.
 *
 * All formatters live here so:
 * - The app uses a consistent date style throughout
 * - Locale changes are handled in one place
 * - Composables never instantiate formatters themselves
 */
object DateUtils {

    /** "3 Mar" — used in WeekNavigationRow header range label. */
    fun dayMonthFormatter(locale: Locale = Locale.getDefault()): SimpleDateFormat =
        SimpleDateFormat("d MMM", locale)

    /** "H:mm" — used for event start time in DayColumn event blocks. */
    fun eventTimeFormatter(locale: Locale = Locale.getDefault()): SimpleDateFormat =
        SimpleDateFormat("H:mm", locale)

    /** "h:mm a" — used for full time display in event detail popups. */
    fun fullTimeFormatter(locale: Locale = Locale.getDefault()): SimpleDateFormat =
        SimpleDateFormat("h:mm a", locale)

    /** "EEE d" — used in DayColumnHeaders for abbreviated day + date. */
    fun dayHeaderFormatter(locale: Locale = Locale.getDefault()): SimpleDateFormat =
        SimpleDateFormat("EEE d", locale)
}
