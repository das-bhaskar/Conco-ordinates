package com.example.myapplication.logic

import android.util.Log
import com.example.myapplication.BuildConfig
import com.example.myapplication.data.CourseScheduleEntry
import com.example.myapplication.data.CourseScheduleResult
import com.example.myapplication.data.CourseUiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.net.HttpURLConnection
import java.util.concurrent.TimeUnit

interface ConcordiaApiRepository {
    suspend fun getCourseSchedule(
        subject:  String = "*",
        catalog:  String = "*",
        termCode: String = "2254"
    ): CourseScheduleResult
}

class ConcordiaApiRepositoryImpl : ConcordiaApiRepository {

    private val apiUser = BuildConfig.CONCORDIA_API_USER
    private val apiKey  = BuildConfig.CONCORDIA_API_KEY

    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG      = "ConcordiaApi"
        private const val BASE_URL = "https://opendata.concordia.ca/API/v1"
        const val CURRENT_TERM     = "2254"
    }

    override suspend fun getCourseSchedule(
        subject:  String,
        catalog:  String,
        termCode: String
    ): CourseScheduleResult = withContext(Dispatchers.IO) {

        // The API only returns data when termCode is wildcard "*".
        // We apply the term filter on the client side after receiving results.
        val url = "$BASE_URL/course/schedule/filter/*/$subject/$catalog"
        Log.d(TAG, "Fetching: $url (client-side filter term=$termCode)")

        try {
            val request = Request.Builder()
                .url(url)
                .header("Authorization", okhttp3.Credentials.basic(apiUser, apiKey))
                .header("Accept", "application/json")
                .build()

            val response = client.newCall(request).execute()
            Log.d(TAG, "HTTP ${response.code}")

            return@withContext when (response.code) {
                HttpURLConnection.HTTP_OK -> {
                    val body = response.body?.string() ?: "[]"
                    Log.d(TAG, "Response length: ${body.length}, preview: ${body.take(100)}")
                    parseScheduleResponse(body, termCode)
                }
                HttpURLConnection.HTTP_UNAUTHORIZED,
                HttpURLConnection.HTTP_FORBIDDEN -> {
                    Log.e(TAG, "Auth failed: ${response.code}")
                    CourseScheduleResult.AuthError
                }
                else -> {
                    Log.e(TAG, "Server error: ${response.code}")
                    CourseScheduleResult.ServiceUnavailable
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network error: ${e.message}")
            CourseScheduleResult.NetworkError
        }
    }

    private fun parseScheduleResponse(json: String, termCode: String): CourseScheduleResult {
        return try {
            val array = JSONArray(json)
            if (array.length() == 0) return CourseScheduleResult.Empty

            val uiModels = mutableListOf<CourseUiModel>()

            for (i in 0 until array.length()) {
                val obj       = array.getJSONObject(i)
                val entryTerm = obj.optString("termCode")

                // Client-side term filter
                if (termCode != "*" && entryTerm != termCode) continue

                val entry = CourseScheduleEntry(
                    courseID             = obj.optString("courseID"),
                    termCode             = entryTerm,
                    session              = obj.optString("session"),
                    subject              = obj.optString("subject"),
                    catalog              = obj.optString("catalog"),
                    section              = obj.optString("section"),
                    componentCode        = obj.optString("componentCode"),
                    componentDescription = obj.optString("componentDescription"),
                    classNumber          = obj.optString("classNumber"),
                    courseTitle          = obj.optString("courseTitle"),
                    classStatus          = obj.optString("classStatus"),
                    locationCode         = obj.optString("locationCode"),
                    instructionModeCode  = obj.optString("instructionModeCode"),
                    instructionModeDescription = obj.optString("instructionModeDescription"),
                    roomCode             = obj.optString("roomCode"),
                    buildingCode         = obj.optString("buildingCode"),
                    room                 = obj.optString("room"),
                    classStartTime       = obj.optString("classStartTime"),
                    classEndTime         = obj.optString("classEndTime"),
                    mondays              = obj.optString("modays",     "N"),
                    tuesdays             = obj.optString("tuesdays",   "N"),
                    wednesdays           = obj.optString("wednesdays", "N"),
                    thursdays            = obj.optString("thursdays",  "N"),
                    fridays              = obj.optString("fridays",    "N"),
                    saturdays            = obj.optString("saturdays",  "N"),
                    sundays              = obj.optString("sundays",    "N"),
                    classStartDate       = obj.optString("classStartDate"),
                    classEndDate         = obj.optString("classEndDate"),
                    career               = obj.optString("career"),
                    departmentCode       = obj.optString("departmentCode"),
                    departmentDescription = obj.optString("departmentDescription"),
                    facultyCode          = obj.optString("facultyCode"),
                    facultyDescription   = obj.optString("facultyDescription"),
                    enrollmentCapacity   = obj.optString("enrollmentCapacity"),
                    currentEnrollment    = obj.optString("currentEnrollment")
                )
                uiModels.add(entry.toUiModel())
            }

            if (uiModels.isEmpty()) CourseScheduleResult.Empty
            else CourseScheduleResult.Success(uiModels)

        } catch (e: Exception) {
            Log.e(TAG, "Parse error: ${e.message}")
            CourseScheduleResult.NetworkError
        }
    }

    private fun CourseScheduleEntry.toUiModel(): CourseUiModel {
        val days = buildList {
            if (mondays    == "Y") add("Mon")
            if (tuesdays   == "Y") add("Tue")
            if (wednesdays == "Y") add("Wed")
            if (thursdays  == "Y") add("Thu")
            if (fridays    == "Y") add("Fri")
            if (saturdays  == "Y") add("Sat")
            if (sundays    == "Y") add("Sun")
        }.joinToString(" / ")

        fun formatTime(t: String) = t.replace(".", ":").take(5)

        return CourseUiModel(
            courseCode     = "$subject $catalog",
            courseTitle    = courseTitle.ifBlank { "$subject $catalog" },
            section        = section,
            componentType  = componentDescription.ifBlank { componentCode },
            buildingCode   = buildingCode,
            roomNumber     = room,
            locationCampus = locationCode,
            daysOfWeek     = days.ifBlank { "TBD" },
            startTime      = formatTime(classStartTime),
            endTime        = formatTime(classEndTime),
            isOnline       = instructionModeCode == "OL"
        )
    }
}
