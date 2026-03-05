package com.example.myapplication.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.CourseScheduleResult
import com.example.myapplication.data.CourseUiModel
import com.example.myapplication.logic.ConcordiaApiRepository
import com.example.myapplication.logic.ConcordiaApiRepositoryImpl
import kotlinx.coroutines.launch

class CourseScheduleViewModel(
    private val repository: ConcordiaApiRepository = ConcordiaApiRepositoryImpl()
) : ViewModel() {

    // ── Search inputs ────────────────────────────────────────────────────────
    var subjectQuery by mutableStateOf("")
        private set
    var catalogQuery by mutableStateOf("")
        private set

    // ── Raw search results bucketed by component type ────────────────────────
    var lectures  by mutableStateOf<List<CourseUiModel>>(emptyList())
        private set
    var tutorials by mutableStateOf<List<CourseUiModel>>(emptyList())
        private set
    var labs      by mutableStateOf<List<CourseUiModel>>(emptyList())
        private set

    // ── Personal saved schedule ──────────────────────────────────────────────
    var myCourses by mutableStateOf<List<CourseUiModel>>(emptyList())
        private set

    // ── UI state ─────────────────────────────────────────────────────────────
    var isLoading    by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var isEmpty      by mutableStateOf(false)
        private set

    // ── Input handlers ───────────────────────────────────────────────────────
    fun onSubjectChanged(value: String) { subjectQuery = value.uppercase().trim() }
    fun onCatalogChanged(value: String) { catalogQuery = value.trim() }

    // ── Search: fetch all sections of a course and bucket by component ───────
    fun searchCourse() {
        if (subjectQuery.isBlank() || catalogQuery.isBlank()) return

        isLoading    = true
        errorMessage = null
        isEmpty      = false
        lectures     = emptyList()
        tutorials    = emptyList()
        labs         = emptyList()

        viewModelScope.launch {
            when (val result = repository.getCourseSchedule(subjectQuery, catalogQuery, "2254")) {
                is CourseScheduleResult.Success -> {
                    lectures  = result.courses.filter { isLecture(it.componentType) }
                        .sortedBy { it.section }
                    tutorials = result.courses.filter { isTutorial(it.componentType) }
                        .sortedBy { it.section }
                    labs      = result.courses.filter { isLab(it.componentType) }
                        .sortedBy { it.section }
                    isEmpty   = lectures.isEmpty() && tutorials.isEmpty() && labs.isEmpty()
                    isLoading = false
                }
                CourseScheduleResult.Empty -> { isEmpty = true; isLoading = false }
                CourseScheduleResult.AuthError -> {
                    errorMessage = "Service unavailable — authentication failed"; isLoading = false
                }
                CourseScheduleResult.ServiceUnavailable -> {
                    errorMessage = "Service unavailable — please try again later"; isLoading = false
                }
                CourseScheduleResult.NetworkError -> {
                    errorMessage = "Network error — check your connection"; isLoading = false
                }
            }
        }
    }

    // Given a chosen LEC section (e.g. "AA"), return matching TUTs/LABs
    // Concordia pattern: LEC "AA" → TUT "AAIA"/"AAJB", LAB "AALA"/"AALB"
    // The matching TUTs/LABs start with the same base letters as the LEC section.
    fun tutorialsFor(lecSection: String): List<CourseUiModel> {
        val base = lecSection.trimEnd { it.isDigit() || it == 'I' || it == 'A' || it == 'B' || it == 'C' }
            .ifBlank { lecSection.take(2) }
        return tutorials.filter { it.section.startsWith(base, ignoreCase = true) }
            .ifEmpty { tutorials }   // fallback: show all if prefix match fails
    }

    fun labsFor(lecSection: String): List<CourseUiModel> {
        val base = lecSection.trimEnd { it.isDigit() || it == 'I' || it == 'A' || it == 'B' || it == 'C' }
            .ifBlank { lecSection.take(2) }
        return labs.filter { it.section.startsWith(base, ignoreCase = true) }
            .ifEmpty { labs }
    }

    // ── Save / remove ────────────────────────────────────────────────────────
    fun addCourse(course: CourseUiModel) {
        val key = course.courseCode + course.section + course.componentType
        if (myCourses.none { it.courseCode + it.section + it.componentType == key }) {
            myCourses = myCourses + course
        }
    }

    fun removeCourse(course: CourseUiModel) {
        myCourses = myCourses.filter {
            !(it.courseCode == course.courseCode
                    && it.section == course.section
                    && it.componentType == course.componentType)
        }
    }

    // ── Utility ──────────────────────────────────────────────────────────────
    fun clearSearch() {
        subjectQuery = ""; catalogQuery = ""
        lectures = emptyList(); tutorials = emptyList(); labs = emptyList()
        isEmpty = false; errorMessage = null
    }

    fun clearError() { errorMessage = null }

    companion object {
        fun isLecture(type: String)  = type.contains("lec", ignoreCase = true)
        fun isTutorial(type: String) = type.contains("tut", ignoreCase = true)
        fun isLab(type: String)      = type.contains("lab", ignoreCase = true)
    }
}
