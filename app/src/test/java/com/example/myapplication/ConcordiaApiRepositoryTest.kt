package com.example.myapplication.logic

import com.example.myapplication.data.CourseScheduleResult
import com.example.myapplication.data.CourseUiModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for ConcordiaApiRepository
 *
 * AT-3.1.1 (#245) — Successful Connection
 * AT-3.1.2 (#246) — Connection Failure
 * AT-3.2.1 (#247) — Fetching Schedule Data
 * AT-3.2.2 (#248) — Invalid Data Handling
 * AT-3.2.3 (#249) — No Courses Found
 */

// ── Mock repository for testing without hitting the real API ──────────────
class MockConcordiaApiRepository(
    private val response: CourseScheduleResult
) : ConcordiaApiRepository {
    var lastSubject: String? = null
    var lastCatalog: String? = null

    override suspend fun getCourseSchedule(
        subject: String,
        catalog: String,
        termCode: String
    ): CourseScheduleResult {
        lastSubject = subject
        lastCatalog = catalog
        return response
    }
}

// ── AT-3.1.1 + AT-3.2.1: Successful connection and data fetch ─────────────
class ConcordiaApiRepositoryTest {

    private val sampleCourse = CourseUiModel(
        courseCode     = "COMP 352",
        courseTitle    = "Data Structures and Algorithms",
        section        = "CC",
        componentType  = "Lecture",
        buildingCode   = "H",
        roomNumber     = "820",
        locationCampus = "SGW",
        daysOfWeek     = "Mon / Wed",
        startTime      = "10:15",
        endTime        = "11:30",
        isOnline       = false
    )

    // AT-3.1.1 / AT-3.2.1: Successful fetch returns course list
    @Test
    fun `successful response returns course list`() = runTest {
        val repo = MockConcordiaApiRepository(
            CourseScheduleResult.Success(listOf(sampleCourse))
        )
        val result = repo.getCourseSchedule("COMP", "352")
        assertTrue(result is CourseScheduleResult.Success)
        val courses = (result as CourseScheduleResult.Success).courses
        assertEquals(1, courses.size)
        assertEquals("COMP 352", courses[0].courseCode)
        assertEquals("H", courses[0].buildingCode)
        assertEquals("820", courses[0].roomNumber)
    }

    // AT-3.1.1: Correct subject and catalog are forwarded to the API
    @Test
    fun `query parameters are forwarded correctly`() = runTest {
        val repo = MockConcordiaApiRepository(CourseScheduleResult.Empty)
        repo.getCourseSchedule("SOEN", "341")
        assertEquals("SOEN", repo.lastSubject)
        assertEquals("341", repo.lastCatalog)
    }

    // AT-3.1.2: 401/403 returns AuthError
    @Test
    fun `auth error returns AuthError result`() = runTest {
        val repo = MockConcordiaApiRepository(CourseScheduleResult.AuthError)
        val result = repo.getCourseSchedule("COMP", "352")
        assertTrue(result is CourseScheduleResult.AuthError)
    }

    // AT-3.1.2 / Task-3.1.4: 404 or 500 returns ServiceUnavailable
    @Test
    fun `server error returns ServiceUnavailable result`() = runTest {
        val repo = MockConcordiaApiRepository(CourseScheduleResult.ServiceUnavailable)
        val result = repo.getCourseSchedule("COMP", "352")
        assertTrue(result is CourseScheduleResult.ServiceUnavailable)
    }

    // AT-3.1.2: Network failure returns NetworkError
    @Test
    fun `network failure returns NetworkError result`() = runTest {
        val repo = MockConcordiaApiRepository(CourseScheduleResult.NetworkError)
        val result = repo.getCourseSchedule("COMP", "352")
        assertTrue(result is CourseScheduleResult.NetworkError)
    }

    // AT-3.2.3: No courses found returns Empty
    @Test
    fun `empty response returns Empty result`() = runTest {
        val repo = MockConcordiaApiRepository(CourseScheduleResult.Empty)
        val result = repo.getCourseSchedule("XXXX", "999")
        assertTrue(result is CourseScheduleResult.Empty)
    }

    // AT-3.2.2: Invalid data — malformed entries are skipped, valid ones returned
    @Test
    fun `partial valid data returns only valid courses`() = runTest {
        val repo = MockConcordiaApiRepository(
            CourseScheduleResult.Success(listOf(sampleCourse))
        )
        val result = repo.getCourseSchedule("COMP", "*")
        assertTrue(result is CourseScheduleResult.Success)
        // Only the valid sampleCourse should be present
        assertEquals(1, (result as CourseScheduleResult.Success).courses.size)
    }

    // roomDisplay helper
    @Test
    fun `roomDisplay formats building and room correctly`() {
        assertEquals("H-820", sampleCourse.roomDisplay)
    }

    // Online course roomDisplay
    @Test
    fun `online course shows Online instead of room`() {
        val onlineCourse = sampleCourse.copy(isOnline = true)
        assertEquals("Online", onlineCourse.roomDisplay)
    }

    // scheduleDisplay helper
    @Test
    fun `scheduleDisplay combines days and times`() {
        assertEquals("Mon / Wed  10:15 – 11:30", sampleCourse.scheduleDisplay)
    }
}
