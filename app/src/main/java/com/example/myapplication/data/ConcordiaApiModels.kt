package com.example.myapplication.data

/**
 * Data models for the Concordia Open Data API
 *
 * US-3.1 Task-3.1.3 (#188) — Data Modeling
 * US-3.2 Task-3.2.2 (#191) — Filter student-specific courses
 *
 * API reference: https://opendata.concordia.ca/API/v1/course/schedule/filter/{term}/{subject}/{catalog}
 *
 * Example response field: buildingCode = "H", room = "820" → Hall Building room 820
 */

// ── Raw API response model (maps directly to JSON fields) ─────────────────
data class CourseScheduleEntry(
    val courseID: String,
    val termCode: String,
    val session: String,
    val subject: String,
    val catalog: String,
    val section: String,
    val componentCode: String,          // LEC, TUT, LAB
    val componentDescription: String,
    val classNumber: String,
    val courseTitle: String,
    val classStatus: String,            // Active / Inactive
    val locationCode: String,           // SGW or LOY
    val instructionModeCode: String,    // P = In Person, OL = Online
    val instructionModeDescription: String,
    val roomCode: String,               // e.g. "H820"
    val buildingCode: String,           // e.g. "H"
    val room: String,                   // e.g. "820"
    val classStartTime: String,         // e.g. "10.15.00"
    val classEndTime: String,           // e.g. "11.30.00"
    val mondays: String,                // "Y" or "N"
    val tuesdays: String,
    val wednesdays: String,
    val thursdays: String,
    val fridays: String,
    val saturdays: String,
    val sundays: String,
    val classStartDate: String,
    val classEndDate: String,
    val career: String,                 // Undergraduate / Graduate
    val departmentCode: String,
    val departmentDescription: String,
    val facultyCode: String,
    val facultyDescription: String,
    val enrollmentCapacity: String,
    val currentEnrollment: String
)

// ── UI-friendly model used in ViewModels and Composables ──────────────────
data class CourseUiModel(
    val courseCode: String,         // e.g. "COMP 352"
    val courseTitle: String,        // e.g. "Data Structures and Algorithms"
    val section: String,            // e.g. "CC"
    val componentType: String,      // e.g. "Lecture"
    val buildingCode: String,       // e.g. "H" — used to highlight on map
    val roomNumber: String,         // e.g. "820"
    val locationCampus: String,     // "SGW" or "Loyola"
    val daysOfWeek: String,         // e.g. "Mon / Wed"
    val startTime: String,          // e.g. "10:15"
    val endTime: String,            // e.g. "11:30"
    val isOnline: Boolean
) {
    /** Full display string for room, e.g. "H-820" */
    val roomDisplay: String get() = if (isOnline) "Online" else "$buildingCode-$roomNumber"

    /** Short schedule string, e.g. "Mon/Wed 10:15–11:30" */
    val scheduleDisplay: String get() = "$daysOfWeek  $startTime – $endTime"
}

// ── Result wrapper — consistent with ShuttleAvailability sealed class pattern ──
sealed class CourseScheduleResult {
    data class Success(val courses: List<CourseUiModel>) : CourseScheduleResult()
    object Empty : CourseScheduleResult()               // Valid response but no courses found
    object NetworkError : CourseScheduleResult()
    object AuthError : CourseScheduleResult()           // 401 — bad API key
    object ServiceUnavailable : CourseScheduleResult()  // 404 / 500
}
