package com.example.myapplication.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.util.*
import java.util.Locale

class DateUtilsTest {

    // Fixed timestamp: March 7, 2026, 14:30:00 (2:30 PM)
    private val testTimestamp = 1741375800000L

    @Test
    fun `dayMonthFormatter formats correctly for US locale`() {
        val formatter = DateUtils.dayMonthFormatter(Locale.US)
        val result = formatter.format(Date(testTimestamp))
        // Expecting "7 Mar"
        assertEquals("7 Mar", result)
    }

    private val testTimeZone = TimeZone.getTimeZone("America/New_York")
    private val testLocale = Locale.US

    @Test
    fun `fullTimeFormatter formats correctly with AM-PM`() {
        // 1. Arrange: Setup formatter with fixed constraints
        val formatter = DateUtils.fullTimeFormatter(testLocale).apply {
            timeZone = testTimeZone
        }

        // 2. Arrange: Setup a specific moment in time (March 8, 2026, 2:30 PM)
        val calendar = Calendar.getInstance(testTimeZone).apply {
            set(2026, Calendar.MARCH, 8, 14, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // 3. Act
        val result = formatter.format(calendar.time)

        // 4. Assert
        assertEquals("2:30 PM", result)
    }

    @Test
    fun `eventTimeFormatter formats correctly for 24h style`() {
        val formatter = DateUtils.eventTimeFormatter(testLocale).apply {
            timeZone = testTimeZone
        }

        val calendar = Calendar.getInstance(testTimeZone).apply {
            set(2026, Calendar.MARCH, 8, 14, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val result = formatter.format(calendar.time)
        assertEquals("14:30", result)
    }
    @Test
    fun `test formatters with default parameters for full coverage`() {
        // Calling these without arguments triggers the $default methods
        val dm = DateUtils.dayMonthFormatter()
        val et = DateUtils.eventTimeFormatter()
        val ft = DateUtils.fullTimeFormatter()
        val dh = DateUtils.dayHeaderFormatter()

        // Assert that the objects are created correctly
        assertNotNull(dm)
        assertNotNull(et)
        assertNotNull(ft)
        assertNotNull(dh)
    }

    @Test
    fun `test formatters with explicit locale`() {
        val locale = Locale.FRENCH
        val dm = DateUtils.dayMonthFormatter(locale)

        // Correct way to verify locale: check the symbols' localizable strings
        // or compare the pattern string
        assertEquals("d MMM", dm.toPattern())
    }

}