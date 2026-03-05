package com.example.myapplication.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.CourseUiModel
import com.example.myapplication.ui.theme.ConcordiaMaroon
import com.example.myapplication.ui.viewmodel.CourseScheduleViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

// ── Layout constants ──────────────────────────────────────────────────────────
private val TIME_COL_W : Dp = 44.dp
private val DAY_COL_W  : Dp = 52.dp
private val HOUR_H     : Dp = 60.dp
private val GRID_START : Int = 8    // 08:00
private val GRID_END   : Int = 21   // 21:00

private val DAY_ABBR  = listOf("Mon", "Tue", "Wed", "Thu", "Fri")
private val DAY_LETTER = listOf("M", "T", "W", "T", "F")

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun componentColor(type: String) = when {
    CourseScheduleViewModel.isLecture(type)  -> Color(0xFF1B5E20)
    CourseScheduleViewModel.isLab(type)      -> Color(0xFF0D47A1)
    CourseScheduleViewModel.isTutorial(type) -> Color(0xFF4A148C)
    else                                     -> Color(0xFF37474F)
}

private fun timeToFloat(t: String): Float {
    val p = t.split(":")
    return (p.getOrNull(0)?.toFloatOrNull() ?: 0f) +
           (p.getOrNull(1)?.toFloatOrNull() ?: 0f) / 60f
}

private fun weekDates(): List<LocalDate> {
    val today  = LocalDate.now()
    val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
    return (0..4).map { monday.plusDays(it.toLong()) }
}

private fun todayColIndex(): Int = when (LocalDate.now().dayOfWeek) {
    DayOfWeek.MONDAY    -> 0
    DayOfWeek.TUESDAY   -> 1
    DayOfWeek.WEDNESDAY -> 2
    DayOfWeek.THURSDAY  -> 3
    DayOfWeek.FRIDAY    -> 4
    else                -> -1
}

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun CourseScheduleScreen(
    viewModel:   CourseScheduleViewModel,
    onFindOnMap: (buildingCode: String) -> Unit = {},
    modifier:    Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    val dates    = remember { weekDates() }
    val todayIdx = remember { todayColIndex() }

    Column(modifier = modifier.fillMaxSize()) {

        // ── Header ────────────────────────────────────────────────────────
        Surface(color = ConcordiaMaroon, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Schedule",
                        style      = MaterialTheme.typography.headlineSmall,
                        color      = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    if (dates.isNotEmpty()) {
                        val first = dates.first()
                        val last  = dates.last()
                        val label = "From ${first.dayOfMonth} " +
                            "${first.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)}" +
                            " To ${last.dayOfMonth} " +
                            last.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                        Text(label, style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f))
                    }
                }
                SmallFloatingActionButton(
                    onClick        = { showAddDialog = true },
                    containerColor = Color.White,
                    contentColor   = ConcordiaMaroon
                ) { Icon(Icons.Default.Add, "Add course") }
            }
        }

        // ── Day header row ────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
        ) {
            Spacer(Modifier.width(TIME_COL_W))
            dates.forEachIndexed { i, date ->
                val isToday = i == todayIdx
                Column(
                    modifier            = Modifier.width(DAY_COL_W).padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        DAY_LETTER[i],
                        style      = MaterialTheme.typography.labelSmall,
                        color      = if (isToday) ConcordiaMaroon else Color.Gray,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                    )
                    Box(
                        modifier         = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(if (isToday) ConcordiaMaroon else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${date.dayOfMonth}",
                            style      = MaterialTheme.typography.bodySmall,
                            color      = if (isToday) Color.White else Color.DarkGray,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        Divider(color = Color(0xFFDDDDDD), thickness = 0.5.dp)

        // ── Grid ──────────────────────────────────────────────────────────
        if (viewModel.myCourses.isEmpty()) {
            EmptyState(onAddClick = { showAddDialog = true })
        } else {
            WeekGrid(
                courses  = viewModel.myCourses,
                todayIdx = todayIdx,
                onRemove = { viewModel.removeCourse(it) }
            )
        }
    }

    if (showAddDialog) {
        AddCourseDialog(
            viewModel = viewModel,
            onDismiss = { showAddDialog = false; viewModel.clearSearch() }
        )
    }
}

// ── Week Grid ─────────────────────────────────────────────────────────────────

@Composable
private fun WeekGrid(
    courses:  List<CourseUiModel>,
    todayIdx: Int,
    onRemove: (CourseUiModel) -> Unit
) {
    val totalH = HOUR_H * (GRID_END - GRID_START)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {

            // Time labels
            Column(modifier = Modifier.width(TIME_COL_W)) {
                (GRID_START until GRID_END).forEach { hour ->
                    Box(modifier = Modifier.height(HOUR_H), contentAlignment = Alignment.TopEnd) {
                        Text(
                            "${hour.toString().padStart(2,'0')}:00",
                            style    = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color    = Color(0xFFAAAAAA),
                            modifier = Modifier.padding(end = 6.dp, top = 2.dp)
                        )
                    }
                }
            }

            // Day columns
            DAY_ABBR.forEachIndexed { dayIdx, dayAbbr ->
                val isToday    = dayIdx == todayIdx
                val dayCourses = courses.filter { dayAbbr in it.daysOfWeek }

                Box(
                    modifier = Modifier
                        .width(DAY_COL_W)
                        .height(totalH)
                        .background(
                            if (isToday) ConcordiaMaroon.copy(alpha = 0.05f)
                            else Color.Transparent
                        )
                ) {
                    // Hour lines
                    Column(modifier = Modifier.fillMaxSize()) {
                        repeat(GRID_END - GRID_START) {
                            Divider(
                                color     = Color(0xFFEEEEEE),
                                thickness = 0.5.dp,
                                modifier  = Modifier.height(HOUR_H)
                            )
                        }
                    }

                    // Course blocks
                    dayCourses.forEach { course ->
                        val startF  = timeToFloat(course.startTime).coerceIn(
                            GRID_START.toFloat(), GRID_END.toFloat())
                        val endF    = timeToFloat(course.endTime).coerceIn(
                            GRID_START.toFloat(), GRID_END.toFloat())
                        val topPx   = ((startF - GRID_START) * HOUR_H.value).dp
                        val blockH  = ((endF - startF) * HOUR_H.value).dp.coerceAtLeast(28.dp)
                        val color   = componentColor(course.componentType)

                        Box(
                            modifier = Modifier
                                .padding(horizontal = 2.dp)
                                .offset(y = topPx)
                                .width(DAY_COL_W - 4.dp)
                                .height(blockH)
                                .clip(RoundedCornerShape(4.dp))
                                .background(color)
                                .clickable { onRemove(course) }
                                .padding(3.dp)
                        ) {
                            Column {
                                Text(
                                    course.courseCode,
                                    style      = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color      = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    maxLines   = 2,
                                    overflow   = TextOverflow.Ellipsis
                                )
                                Text(
                                    course.componentType.take(3).uppercase(),
                                    style   = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                    color   = Color.White.copy(alpha = 0.85f),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Add Course Dialog — 3-step LEC → TUT → LAB ───────────────────────────────

private enum class AddStep { INPUT, LECTURE, TUTORIAL, LAB }

@Composable
private fun AddCourseDialog(
    viewModel: CourseScheduleViewModel,
    onDismiss: () -> Unit
) {
    var step        by remember { mutableStateOf(AddStep.INPUT) }
    var selectedLec by remember { mutableStateOf<CourseUiModel?>(null) }
    var selectedTut by remember { mutableStateOf<CourseUiModel?>(null) }
    var selectedLab by remember { mutableStateOf<CourseUiModel?>(null) }

    // When search results arrive, move to LECTURE step
    val hasResults = viewModel.lectures.isNotEmpty() ||
                     viewModel.tutorials.isNotEmpty() ||
                     viewModel.labs.isNotEmpty()

    LaunchedEffect(hasResults) {
        if (hasResults && step == AddStep.INPUT) {
            step = AddStep.LECTURE
        }
    }

    // Filtered TUT/LAB for the chosen LEC
    val relatedTuts = selectedLec?.let { viewModel.tutorialsFor(it.section) }
        ?: viewModel.tutorials
    val relatedLabs = selectedLec?.let { viewModel.labsFor(it.section) }
        ?: viewModel.labs

    fun doFinish() {
        selectedLec?.let { viewModel.addCourse(it) }
        selectedTut?.let { viewModel.addCourse(it) }
        selectedLab?.let { viewModel.addCourse(it) }
        onDismiss()
    }

    fun advanceFromLec() {
        step = when {
            relatedTuts.isNotEmpty() -> AddStep.TUTORIAL
            relatedLabs.isNotEmpty() -> AddStep.LAB
            else                     -> { doFinish(); AddStep.INPUT }
        }
    }

    fun advanceFromTut() {
        step = when {
            relatedLabs.isNotEmpty() -> AddStep.LAB
            else                     -> { doFinish(); AddStep.INPUT }
        }
    }

    val title = when (step) {
        AddStep.INPUT    -> "Add Course"
        AddStep.LECTURE  -> "Select Lecture"
        AddStep.TUTORIAL -> "Select Tutorial  (${selectedLec?.section ?: ""})"
        AddStep.LAB      -> "Select Lab  (${selectedLec?.section ?: ""})"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title   = { Text(title, fontWeight = FontWeight.Bold) },
        text    = {
            Column(
                modifier            = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (step) {

                    // ── Step 0: search inputs ────────────────────────────
                    AddStep.INPUT -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value         = viewModel.subjectQuery,
                                onValueChange = { viewModel.onSubjectChanged(it) },
                                label         = { Text("Subject") },
                                placeholder   = { Text("SOEN") },
                                modifier      = Modifier.weight(1.2f),
                                singleLine    = true,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Characters,
                                    imeAction      = ImeAction.Next
                                )
                            )
                            OutlinedTextField(
                                value         = viewModel.catalogQuery,
                                onValueChange = { viewModel.onCatalogChanged(it) },
                                label         = { Text("Number") },
                                placeholder   = { Text("228") },
                                modifier      = Modifier.weight(1f),
                                singleLine    = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction    = ImeAction.Search
                                ),
                                keyboardActions = KeyboardActions(
                                    onSearch = { viewModel.searchCourse() }
                                )
                            )
                        }
                        if (viewModel.isLoading) {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                                color    = ConcordiaMaroon
                            )
                        }
                        viewModel.errorMessage?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error)
                        }
                        if (viewModel.isEmpty && !viewModel.isLoading) {
                            Text(
                                "No courses found — check subject and number",
                                style    = MaterialTheme.typography.bodySmall,
                                color    = Color.Gray,
                                modifier = Modifier.testTag("empty_state")
                            )
                        }
                    }

                    // ── Step 1: pick LEC ─────────────────────────────────
                    AddStep.LECTURE -> {
                        if (viewModel.lectures.isEmpty()) {
                            Text("No lectures found — try selecting a tutorial or lab directly.",
                                style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        viewModel.lectures.forEach { lec ->
                            SectionRow(
                                course     = lec,
                                isSelected = selectedLec == lec,
                                onSelect   = { selectedLec = lec }
                            )
                        }
                    }

                    // ── Step 2: pick TUT ─────────────────────────────────
                    AddStep.TUTORIAL -> {
                        relatedTuts.forEach { tut ->
                            SectionRow(
                                course     = tut,
                                isSelected = selectedTut == tut,
                                onSelect   = { selectedTut = tut }
                            )
                        }
                    }

                    // ── Step 3: pick LAB ─────────────────────────────────
                    AddStep.LAB -> {
                        relatedLabs.forEach { lab ->
                            SectionRow(
                                course     = lab,
                                isSelected = selectedLab == lab,
                                onSelect   = { selectedLab = lab }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            when (step) {
                AddStep.INPUT -> Button(
                    onClick  = { viewModel.searchCourse() },
                    enabled  = !viewModel.isLoading && viewModel.subjectQuery.isNotBlank() && viewModel.catalogQuery.isNotBlank(),
                    colors   = ButtonDefaults.buttonColors(containerColor = ConcordiaMaroon)
                ) { Text("Search") }

                AddStep.LECTURE -> Button(
                    onClick  = { advanceFromLec() },
                    enabled  = selectedLec != null,
                    colors   = ButtonDefaults.buttonColors(containerColor = ConcordiaMaroon)
                ) { Text("Next") }

                AddStep.TUTORIAL -> Button(
                    onClick  = { advanceFromTut() },
                    enabled  = selectedTut != null,
                    colors   = ButtonDefaults.buttonColors(containerColor = ConcordiaMaroon)
                ) { Text(if (relatedLabs.isNotEmpty()) "Next" else "Add") }

                AddStep.LAB -> Button(
                    onClick  = { doFinish() },
                    enabled  = selectedLab != null,
                    colors   = ButtonDefaults.buttonColors(containerColor = ConcordiaMaroon)
                ) { Text("Add") }
            }
        },
        dismissButton = {
            when (step) {
                AddStep.INPUT -> TextButton(onClick = onDismiss) { Text("Cancel") }
                AddStep.LECTURE -> TextButton(onClick = onDismiss) { Text("Cancel") }

                AddStep.TUTORIAL -> Row {
                    TextButton(onClick = { step = AddStep.LECTURE }) { Text("Back") }
                    TextButton(onClick = {
                        selectedTut = null
                        if (relatedLabs.isNotEmpty()) step = AddStep.LAB else doFinish()
                    }) { Text("Skip", color = Color.Gray) }
                }

                AddStep.LAB -> Row {
                    TextButton(onClick = {
                        step = if (relatedTuts.isNotEmpty()) AddStep.TUTORIAL else AddStep.LECTURE
                    }) { Text("Back") }
                    TextButton(onClick = { selectedLab = null; doFinish() }) {
                        Text("Skip", color = Color.Gray)
                    }
                }
            }
        }
    )
}

// ── Section selection row ─────────────────────────────────────────────────────

@Composable
private fun SectionRow(
    course:     CourseUiModel,
    isSelected: Boolean,
    onSelect:   () -> Unit
) {
    val color = componentColor(course.componentType)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .border(
                width = if (isSelected) 2.dp else 0.5.dp,
                color = if (isSelected) color else Color(0xFFDDDDDD),
                shape = RoundedCornerShape(10.dp)
            ),
        colors    = CardDefaults.cardColors(
            containerColor = if (isSelected) color.copy(alpha = 0.07f) else Color.White
        ),
        elevation = CardDefaults.cardElevation(0.dp),
        shape     = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier          = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Colour dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Section ${course.section}",
                        style      = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = color.copy(alpha = 0.12f)
                    ) {
                        Text(
                            course.componentType,
                            style    = MaterialTheme.typography.labelSmall,
                            color    = color,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    course.scheduleDisplay,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Text(
                    course.roomDisplay,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            RadioButton(
                selected = isSelected,
                onClick  = onSelect,
                colors   = RadioButtonDefaults.colors(selectedColor = color)
            )
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun EmptyState(onAddClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier            = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "No courses added yet",
                style      = MaterialTheme.typography.titleMedium,
                color      = Color.Gray,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Tap + to add your courses for this semester",
                style     = MaterialTheme.typography.bodyMedium,
                color     = Color.Gray.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onAddClick,
                colors  = ButtonDefaults.buttonColors(containerColor = ConcordiaMaroon)
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Add Course")
            }
        }
    }
}
