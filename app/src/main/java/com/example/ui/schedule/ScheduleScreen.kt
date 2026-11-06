package com.example.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CourseEntity
import com.example.data.model.TimetableSessionEntity
import com.example.ui.JimViewModel
import com.example.ui.components.CourseIconBadge
import com.example.ui.components.BulkSelectionActions
import com.example.ui.components.BulkSelectionIndicator
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.library.shareText
import com.example.ui.theme.JimPrimaryBlue

@Composable
fun ScheduleScreen(
    viewModel: JimViewModel,
    onOpenImport: () -> Unit,
    onOpenCourse: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedDay by viewModel.selectedDayOfWeek.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val courses by viewModel.courses.collectAsState()

    var showAddSessionDialog by remember { mutableStateOf(false) }
    val selectedSessionIds = remember { mutableStateListOf<Long>() }
    var selectionMode by remember { mutableStateOf(false) }
    var sessionsPendingDeletion by remember { mutableStateOf<List<TimetableSessionEntity>>(emptyList()) }

    val days = listOf(
        1 to "الأحد",
        2 to "الإثنين",
        3 to "الثلاثاء",
        4 to "الأربعاء",
        5 to "الخميس"
    )

    val currentDayName = days.find { it.first == selectedDay }?.second ?: "الأحد"
    val daySessions = sessions.filter { it.dayOfWeek == selectedDay }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("schedule_screen")
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Top Header matching screenshot
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Import schedule button
            IconButton(
                onClick = onOpenImport,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .testTag("import_schedule_button")
            ) {
                Icon(
                    imageVector = Icons.Default.FileDownload,
                    contentDescription = "استيراد جدول",
                    tint = JimPrimaryBlue
                )
            }

            Text(
                text = "الجدول الجامعي",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        selectionMode = !selectionMode
                        selectedSessionIds.clear()
                    },
                    modifier = Modifier.testTag("select_sessions_button")
                ) {
                    Icon(
                        Icons.Default.Checklist,
                        contentDescription = "تحديد المحاضرات",
                        tint = if (selectionMode) JimPrimaryBlue else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = { showAddSessionDialog = true },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .testTag("add_session_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "إضافة محاضرة",
                        tint = JimPrimaryBlue
                    )
                }
            }
        }

        if (selectionMode) {
            BulkSelectionActions(
                selectedCount = selectedSessionIds.size,
                onSelectAll = {
                    selectedSessionIds.clear()
                    selectedSessionIds.addAll(daySessions.map { it.id })
                },
                onShare = {
                    val selectedSessions = sessions.filter { it.id in selectedSessionIds }
                    shareText(
                        context,
                        "مشاركة المحاضرات المحددة",
                        selectedSessions.joinToString("\n\n") { session ->
                            val dayName = days.firstOrNull { it.first == session.dayOfWeek }?.second.orEmpty()
                            val courseName = courses.firstOrNull { it.id == session.courseId }?.name.orEmpty()
                            "$courseName - $dayName\n${session.startTime} - ${session.endTime}" +
                                session.classroom.takeIf { it.isNotBlank() }?.let { "\n$it" }.orEmpty()
                        }
                    )
                },
                onDelete = {
                    sessionsPendingDeletion = sessions.filter { it.id in selectedSessionIds }
                },
                onCancel = {
                    selectedSessionIds.clear()
                    selectionMode = false
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Days selector pills (Sunday -> Thursday) matching the screenshot
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            days.forEach { (dayIndex, dayLabel) ->
                val isSelected = selectedDay == dayIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) JimPrimaryBlue else Color.Transparent
                        )
                        .clickable { viewModel.selectScheduleDay(dayIndex) }
                        .padding(vertical = 10.dp)
                        .testTag("schedule_day_tab_$dayIndex"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = dayLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Selected Day Date Subtitle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "جدول يوم $currentDayName",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "${daySessions.size} محاضرات / معامل",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Timetable sessions list
        if (daySessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "لا توجد محاضرات مجدولة لهذا اليوم.\nيمكنك إضافة حصة يدوياً أو استيراد الجدول بنقرة واحدة.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(daySessions) { session ->
                    val course = courses.find { it.id == session.courseId }
                    TimetableSessionItem(
                        session = session,
                        course = course,
                        selectionMode = selectionMode,
                        isSelected = session.id in selectedSessionIds,
                        onToggleSelected = {
                            if (!selectedSessionIds.remove(session.id)) selectedSessionIds.add(session.id)
                        },
                        onOpenCourse = { if (!selectionMode) course?.let { onOpenCourse(it.id) } },
                        onDelete = { viewModel.deleteSession(session) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }

    sessionsPendingDeletion.takeIf { it.isNotEmpty() }?.let { selectedSessions ->
        ConfirmDeleteDialog(
            title = "حذف المحاضرات المحددة",
            message = "سيتم حذف ${selectedSessions.size} محاضرات من الجدول.",
            onDismiss = { sessionsPendingDeletion = emptyList() },
            onConfirm = {
                selectedSessions.forEach(viewModel::deleteSession)
                selectedSessionIds.clear()
                selectionMode = false
                sessionsPendingDeletion = emptyList()
            }
        )
    }

    if (showAddSessionDialog) {
        AddSessionDialog(
            courses = courses,
            initialDay = selectedDay,
            onDismiss = { showAddSessionDialog = false },
            onConfirm = { courseName, day, start, end, room, type, onResult ->
                viewModel.addSession(courseName, day, start, end, room, type) { result ->
                    onResult(result)
                    if (result.isSuccess) showAddSessionDialog = false
                }
            }
        )
    }
}

@Composable
fun TimetableSessionItem(
    session: TimetableSessionEntity,
    course: CourseEntity?,
    selectionMode: Boolean,
    isSelected: Boolean,
    onToggleSelected: () -> Unit,
    onOpenCourse: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { if (selectionMode) onToggleSelected() else onOpenCourse() }
            .testTag("session_item_${session.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectionMode) {
                BulkSelectionIndicator(isSelected)
                Spacer(modifier = Modifier.width(10.dp))
            }
            CourseIconBadge(
                iconKey = course?.iconKey ?: "computer",
                colorHex = course?.colorHex ?: "#2563EB",
                size = 46.dp
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = course?.name ?: "مقرر دراسي",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "${session.startTime} - ${session.endTime}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (session.classroom.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "القاعة: ${session.classroom}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (!selectionMode) Column(horizontalAlignment = Alignment.End) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.background)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = session.sessionType,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "حذف",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSessionDialog(
    courses: List<CourseEntity>,
    initialDay: Int,
    onDismiss: () -> Unit,
    onConfirm: (String, Int, String, String, String, String, (Result<Unit>) -> Unit) -> Unit
) {
    var courseName by remember { mutableStateOf("") }
    var courseMenuExpanded by remember { mutableStateOf(false) }
    var selectedDay by remember { mutableIntStateOf(initialDay) }
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    var classroom by remember { mutableStateOf("") }
    var sessionType by remember { mutableStateOf("محاضرة") }
    var saveError by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    val validTimeRange = com.example.ui.schedule.ScheduleTextParser.hasValidTimeRange(startTime, endTime)
    val matchingCourses = courses.filter {
        courseName.isNotBlank() &&
            (it.name.contains(courseName.trim(), ignoreCase = true) ||
                it.code.contains(courseName.trim(), ignoreCase = true))
    }

    val days = listOf(1 to "الأحد", 2 to "الإثنين", 3 to "الثلاثاء", 4 to "الأربعاء", 5 to "الخميس")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "إضافة محاضرة للجدول",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = courseMenuExpanded && matchingCourses.isNotEmpty(),
                    onExpandedChange = { courseMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = courseName,
                        onValueChange = {
                            courseName = it
                            courseMenuExpanded = true
                        },
                        label = { Text("اسم المادة") },
                        placeholder = { Text("اكتب اسم المادة أو اخترها") },
                        singleLine = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = courseMenuExpanded && matchingCourses.isNotEmpty()
                            )
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = courseMenuExpanded && matchingCourses.isNotEmpty(),
                        onDismissRequest = { courseMenuExpanded = false }
                    ) {
                        matchingCourses.forEach { course ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(course.name)
                                        if (course.code.isNotBlank()) {
                                            Text(
                                                course.code,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    courseName = course.name
                                    courseMenuExpanded = false
                                }
                            )
                        }
                    }
                }
                Text(
                    "إذا لم تكن المادة مسجلة، ستُضاف تلقائيًا إلى قائمة المواد.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text("اليوم:")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    days.forEach { (d, label) ->
                        val isSel = selectedDay == d
                        androidx.compose.material3.FilterChip(
                            onClick = { selectedDay = d },
                            selected = isSel,
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("البداية") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("النهاية") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = classroom,
                        onValueChange = { classroom = it },
                        label = { Text("القاعة / المعمل") },
                        modifier = Modifier.weight(1f)
                    )
                    androidx.compose.material3.OutlinedButton(
                        onClick = { sessionType = if (sessionType == "محاضرة") "معمل" else "محاضرة" },
                        modifier = Modifier.weight(1f).align(Alignment.CenterVertically),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(sessionType)
                    }
                }
                if (startTime.isNotBlank() && endTime.isNotBlank() && !validTimeRange) {
                    Text(
                        text = "تحقق من الوقت: يجب أن يكون بصيغة 08:00 وأن تكون النهاية بعد البداية.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                saveError?.let { error ->
                    Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = courseName.isNotBlank() && validTimeRange && !isSaving,
                onClick = {
                    isSaving = true
                    saveError = null
                    onConfirm(courseName.trim(), selectedDay, startTime, endTime, classroom, sessionType) { result ->
                        isSaving = false
                        if (result.isFailure) {
                            saveError = "تعذر حفظ الحصة. حاول مرة أخرى."
                        }
                    }
                }
            ) {
                Text(if (isSaving) "جارٍ الحفظ..." else "حفظ الحصة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
