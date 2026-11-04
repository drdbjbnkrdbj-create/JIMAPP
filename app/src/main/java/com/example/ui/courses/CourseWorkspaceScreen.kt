package com.example.ui.courses

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AssessmentEntity
import com.example.data.model.AssignmentEntity
import com.example.data.model.CourseEntity
import com.example.data.model.CourseNoteEntity
import com.example.data.model.LectureEntity
import com.example.ui.JimViewModel
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.BulkSelectionActions
import com.example.ui.components.BulkSelectionIndicator
import com.example.ui.components.CourseIconBadge
import com.example.ui.components.PriorityBadge
import com.example.ui.library.rememberDocumentImportLauncher
import com.example.ui.library.shareDocuments
import com.example.ui.library.shareText
import com.example.ui.theme.JimCyanAccent
import com.example.ui.theme.JimPrimaryBlue

enum class WorkspaceTab(val title: String) {
    GRADES("الدرجات"),
    LECTURES("المحاضرات"),
    NOTES("الملاحظات"),
    ASSIGNMENTS("المهام"),
    FILES("الملفات"),
    INFO("المعلومات")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseWorkspaceScreen(
    courseId: Long,
    viewModel: JimViewModel,
    onBack: () -> Unit,
    onOpenFile: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val courses by viewModel.courses.collectAsState()
    val course = courses.find { it.id == courseId } ?: return

    val assessments by viewModel.assessments.collectAsState()
    val courseAssessments = assessments.filter { it.courseId == courseId }

    val lectures by viewModel.lectures.collectAsState()
    val courseLectures = lectures.filter { it.courseId == courseId }

    val assignments by viewModel.assignments.collectAsState()
    val courseAssignments = assignments.filter { it.courseId == courseId }

    val notes by viewModel.notes.collectAsState()
    val courseNotes = notes.filter { it.courseId == courseId }

    val documents by viewModel.documents.collectAsState()
    val courseDocs = documents.filter { it.courseId == courseId }

    var selectedTab by remember { mutableStateOf(WorkspaceTab.GRADES) }
    val selectedItemIds = remember { mutableStateListOf<Long>() }
    var selectionMode by remember { mutableStateOf(false) }
    var showBulkDeleteDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val importCourseDocument = rememberDocumentImportLauncher(viewModel, courseId)

    fun visibleIds(): List<Long> = when (selectedTab) {
        WorkspaceTab.GRADES -> courseAssessments.map { it.id }
        WorkspaceTab.LECTURES -> courseLectures.map { it.id }
        WorkspaceTab.NOTES -> courseNotes.map { it.id }
        WorkspaceTab.ASSIGNMENTS -> courseAssignments.map { it.id }
        WorkspaceTab.FILES -> courseDocs.map { it.id }
        WorkspaceTab.INFO -> emptyList()
    }

    fun deleteSelectedItems() {
        when (selectedTab) {
            WorkspaceTab.GRADES -> courseAssessments.filter { it.id in selectedItemIds }.forEach(viewModel::deleteAssessment)
            WorkspaceTab.LECTURES -> courseLectures.filter { it.id in selectedItemIds }.forEach(viewModel::deleteLecture)
            WorkspaceTab.NOTES -> courseNotes.filter { it.id in selectedItemIds }.forEach(viewModel::deleteNote)
            WorkspaceTab.ASSIGNMENTS -> courseAssignments.filter { it.id in selectedItemIds }.forEach(viewModel::deleteAssignment)
            WorkspaceTab.FILES -> courseDocs.filter { it.id in selectedItemIds }.forEach(viewModel::deleteDocument)
            WorkspaceTab.INFO -> Unit
        }
        selectedItemIds.clear()
        selectionMode = false
    }

    fun shareSelectedItems() {
        if (selectedTab == WorkspaceTab.FILES) {
            shareDocuments(context, courseDocs.filter { it.id in selectedItemIds })
            return
        }

        val selectedText = when (selectedTab) {
            WorkspaceTab.GRADES -> courseAssessments.filter { it.id in selectedItemIds }
                .joinToString("\n\n") { "${it.name}\n${it.earnedScore} / ${it.maxScore}\n${it.date}" }
            WorkspaceTab.LECTURES -> courseLectures.filter { it.id in selectedItemIds }
                .joinToString("\n\n") { "المحاضرة ${it.lectureNumber}: ${it.title}\n${it.summary}" }
            WorkspaceTab.NOTES -> courseNotes.filter { it.id in selectedItemIds }
                .joinToString("\n\n") { "${it.title}\n${it.content}" }
            WorkspaceTab.ASSIGNMENTS -> courseAssignments.filter { it.id in selectedItemIds }
                .joinToString("\n\n") { "${it.title}\n${it.description}\n${it.dueDate}" }
            WorkspaceTab.FILES, WorkspaceTab.INFO -> ""
        }
        shareText(context, "مشاركة ${selectedTab.title} المحددة", selectedText)
    }

    // Dialog states
    var showAddAssessmentDialog by remember { mutableStateOf(false) }
    var showAddLectureDialog by remember { mutableStateOf(false) }
    var showAddAssignmentDialog by remember { mutableStateOf(false) }
    var showAddNoteDialog by remember { mutableStateOf(false) }

    // Grade calculations
    val totalEarned = courseAssessments.sumOf { it.earnedScore }
    val totalMax = courseAssessments.sumOf { it.maxScore }.let { if (it == 0.0) 100.0 else it }
    val percentage = ((totalEarned / totalMax) * 100).coerceIn(0.0, 100.0)
    val letterGrade = when {
        percentage >= 95 -> "A+"
        percentage >= 90 -> "A"
        percentage >= 85 -> "A-"
        percentage >= 80 -> "B+"
        percentage >= 75 -> "B"
        percentage >= 70 -> "B-"
        percentage >= 65 -> "C+"
        percentage >= 60 -> "C"
        else -> "D"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("course_workspace_screen")
    ) {
        // Workspace Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("workspace_back_button")) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "الرجوع",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = course.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${course.code} | ${course.creditHours} ساعات معتمدة",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                CourseIconBadge(iconKey = course.iconKey, colorHex = course.colorHex, size = 40.dp)
                if (selectedTab != WorkspaceTab.INFO) {
                    IconButton(
                        onClick = {
                            selectionMode = !selectionMode
                            selectedItemIds.clear()
                        }
                    ) {
                        Icon(
                            Icons.Default.Checklist,
                            contentDescription = "تحديد عناصر المادة",
                            tint = if (selectionMode) JimPrimaryBlue else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Header Card with Grade / Progress (Matching screenshot)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "الدرجة الحالية",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = letterGrade,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "إجمالي الدرجات",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${percentage.toInt()}%",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = (percentage / 100f).toFloat().coerceIn(0.05f, 1f))
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            Brush.horizontalGradient(listOf(JimPrimaryBlue, JimCyanAccent))
                        )
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Horizontal Scrollable Tabs Row (الدرجات, المحاضرات, الملاحظات, المهام, الملفات, معلومات)
        ScrollableTabRow(
            selectedTabIndex = selectedTab.ordinal,
            edgePadding = 16.dp,
            containerColor = MaterialTheme.colorScheme.background,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                    color = JimPrimaryBlue
                )
            }
        ) {
            WorkspaceTab.values().forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = {
                        selectedTab = tab
                        selectedItemIds.clear()
                        selectionMode = false
                    },
                    text = {
                        Text(
                            text = tab.title,
                            fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == tab) JimPrimaryBlue else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }

        if (selectionMode) {
            BulkSelectionActions(
                selectedCount = selectedItemIds.size,
                onSelectAll = {
                    selectedItemIds.clear()
                    selectedItemIds.addAll(visibleIds())
                },
                onDelete = { showBulkDeleteDialog = true },
                onShare = ::shareSelectedItems,
                onCancel = {
                    selectedItemIds.clear()
                    selectionMode = false
                }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tab Content
        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTab) {
                WorkspaceTab.GRADES -> GradesTabContent(
                    assessments = courseAssessments,
                    selectionMode = selectionMode,
                    selectedIds = selectedItemIds,
                    onToggleSelection = { id ->
                        if (!selectedItemIds.remove(id)) selectedItemIds.add(id)
                    },
                    onAddClick = { showAddAssessmentDialog = true },
                    onDelete = { viewModel.deleteAssessment(it) }
                )
                WorkspaceTab.LECTURES -> LecturesTabContent(
                    lectures = courseLectures,
                    selectionMode = selectionMode,
                    selectedIds = selectedItemIds,
                    onToggleSelection = { id ->
                        if (!selectedItemIds.remove(id)) selectedItemIds.add(id)
                    },
                    onAddClick = { showAddLectureDialog = true },
                    onToggleReviewed = { viewModel.toggleLectureReviewed(it) },
                    onDelete = { viewModel.deleteLecture(it) }
                )
                WorkspaceTab.NOTES -> CourseNotesTabContent(
                    notes = courseNotes,
                    selectionMode = selectionMode,
                    selectedIds = selectedItemIds,
                    onToggleSelection = { id ->
                        if (!selectedItemIds.remove(id)) selectedItemIds.add(id)
                    },
                    onAddClick = { showAddNoteDialog = true },
                    onDelete = { viewModel.deleteNote(it) }
                )
                WorkspaceTab.ASSIGNMENTS -> AssignmentsTabContent(
                    assignments = courseAssignments,
                    selectionMode = selectionMode,
                    selectedIds = selectedItemIds,
                    onToggleSelection = { id ->
                        if (!selectedItemIds.remove(id)) selectedItemIds.add(id)
                    },
                    onAddClick = { showAddAssignmentDialog = true },
                    onToggle = { viewModel.toggleAssignmentStatus(it) },
                    onDelete = { viewModel.deleteAssignment(it) }
                )
                WorkspaceTab.FILES -> CourseFilesTabContent(
                    documents = courseDocs,
                    selectionMode = selectionMode,
                    selectedIds = selectedItemIds,
                    onToggleSelection = { id ->
                        if (!selectedItemIds.remove(id)) selectedItemIds.add(id)
                    },
                    onImportFile = importCourseDocument,
                    onOpenFile = onOpenFile,
                    onDelete = { viewModel.deleteDocument(it) }
                )
                WorkspaceTab.INFO -> CourseInfoTabContent(course = course)
            }
        }
    }

    if (showBulkDeleteDialog) {
        ConfirmDeleteDialog(
            title = "حذف العناصر المحددة",
            message = "سيتم حذف ${selectedItemIds.size} عناصر من ${selectedTab.title}. لا يمكن التراجع عن ذلك.",
            onDismiss = { showBulkDeleteDialog = false },
            onConfirm = {
                deleteSelectedItems()
                showBulkDeleteDialog = false
            }
        )
    }

    if (showAddAssessmentDialog) {
        AddAssessmentDialog(
            onDismiss = { showAddAssessmentDialog = false },
            onConfirm = { name, earned, max, weight, date, notes ->
                viewModel.addAssessment(courseId, name, earned, max, weight, date, notes)
                showAddAssessmentDialog = false
            }
        )
    }

    if (showAddLectureDialog) {
        AddLectureDialog(
            nextNumber = courseLectures.size + 1,
            onDismiss = { showAddLectureDialog = false },
            onConfirm = { title, num, date, summary, instNotes, myNotes ->
                viewModel.addLecture(courseId, title, num, date, summary, instNotes, myNotes)
                showAddLectureDialog = false
            }
        )
    }

    if (showAddAssignmentDialog) {
        AddAssignmentDialog(
            onDismiss = { showAddAssignmentDialog = false },
            onConfirm = { title, desc, dueDate, priority ->
                viewModel.addAssignment(courseId, title, desc, dueDate, priority)
                showAddAssignmentDialog = false
            }
        )
    }

    if (showAddNoteDialog) {
        AddNoteDialog(
            onDismiss = { showAddNoteDialog = false },
            onConfirm = { title, content, category ->
                viewModel.addNote(courseId, title, content, category, false)
                showAddNoteDialog = false
            }
        )
    }

}

@Composable
fun GradesTabContent(
    assessments: List<AssessmentEntity>,
    selectionMode: Boolean,
    selectedIds: List<Long>,
    onToggleSelection: (Long) -> Unit,
    onAddClick: () -> Unit,
    onDelete: (AssessmentEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "التقييمات والاختبارات",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Button(onClick = onAddClick) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إضافة تقييم")
                }
            }
        }

        if (assessments.isEmpty()) {
            item {
                Text(
                    text = "لا توجد تقييمات مضافة بعد لهذه المادة.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            }
        } else {
            items(assessments) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { if (selectionMode) onToggleSelection(item.id) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selectionMode) {
                            BulkSelectionIndicator(item.id in selectedIds)
                        }
                        Column {
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            if (item.date.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = item.date,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${item.earnedScore} / ${item.maxScore}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            if (!selectionMode) {
                                IconButton(onClick = { onDelete(item) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "حذف",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LecturesTabContent(
    lectures: List<LectureEntity>,
    selectionMode: Boolean,
    selectedIds: List<Long>,
    onToggleSelection: (Long) -> Unit,
    onAddClick: () -> Unit,
    onToggleReviewed: (LectureEntity) -> Unit,
    onDelete: (LectureEntity) -> Unit
) {
    var lecturePendingDeletion by remember { mutableStateOf<LectureEntity?>(null) }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "سجل المحاضرات",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Button(onClick = onAddClick) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إضافة محاضرة")
                }
            }
        }

        if (lectures.isEmpty()) {
            item {
                Text(
                    text = "لا توجد محاضرات مدونة بعد.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            }
        } else {
            items(lectures) { lecture ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { if (selectionMode) onToggleSelection(lecture.id) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (selectionMode) {
                                BulkSelectionIndicator(lecture.id in selectedIds)
                            }
                            Text(
                                text = "المحاضرة ${lecture.lectureNumber}: ${lecture.title}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Row {
                                if (!selectionMode) IconButton(onClick = { onToggleReviewed(lecture) }) {
                                    Icon(
                                        imageVector = if (lecture.isReviewed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = "تمت المراجعة",
                                        tint = if (lecture.isReviewed) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (!selectionMode) IconButton(onClick = { lecturePendingDeletion = lecture }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "حذف المحاضرة",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }

                        if (lecture.summary.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = lecture.summary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (lecture.lectureDate.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "التاريخ: ${lecture.lectureDate}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                }
            }
        }
    }

    lecturePendingDeletion?.let { lecture ->
        ConfirmDeleteDialog(
            title = "حذف المحاضرة",
            message = "سيتم حذف المحاضرة «${lecture.title}» نهائيًا.",
            onDismiss = { lecturePendingDeletion = null },
            onConfirm = {
                onDelete(lecture)
                lecturePendingDeletion = null
            }
        )
    }
}

@Composable
fun CourseNotesTabContent(
    notes: List<CourseNoteEntity>,
    selectionMode: Boolean,
    selectedIds: List<Long>,
    onToggleSelection: (Long) -> Unit,
    onAddClick: () -> Unit,
    onDelete: (CourseNoteEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ملاحظات المادة",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Button(onClick = onAddClick) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ملاحظة جديدة")
                }
            }
        }

        if (notes.isEmpty()) {
            item {
                Text(
                    text = "لا توجد ملاحظات مدونة لهذه المادة.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            }
        } else {
            items(notes) { note ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { if (selectionMode) onToggleSelection(note.id) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (selectionMode) {
                                BulkSelectionIndicator(note.id in selectedIds)
                            }
                            Text(
                                text = note.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            if (!selectionMode) IconButton(onClick = { onDelete(note) }, modifier = Modifier.size(24.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "حذف",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = note.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AssignmentsTabContent(
    assignments: List<AssignmentEntity>,
    selectionMode: Boolean,
    selectedIds: List<Long>,
    onToggleSelection: (Long) -> Unit,
    onAddClick: () -> Unit,
    onToggle: (AssignmentEntity) -> Unit,
    onDelete: (AssignmentEntity) -> Unit
) {
    var assignmentPendingDeletion by remember { mutableStateOf<AssignmentEntity?>(null) }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "واجبات ومهام المادة",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Button(onClick = onAddClick) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إضافة واجب")
                }
            }
        }

        if (assignments.isEmpty()) {
            item {
                Text(
                    text = "لا توجد واجبات متبقية لهذه المادة.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            }
        } else {
            items(assignments) { assignment ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { if (selectionMode) onToggleSelection(assignment.id) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selectionMode) {
                            BulkSelectionIndicator(assignment.id in selectedIds)
                        } else {
                            IconButton(onClick = { onToggle(assignment) }) {
                                Icon(
                                    imageVector = if (assignment.status == "COMPLETED") Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (assignment.status == "COMPLETED") Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = assignment.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "تاريخ التسليم: ${assignment.dueDate}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        PriorityBadge(priority = assignment.priority)
                        if (!selectionMode) IconButton(onClick = { assignmentPendingDeletion = assignment }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "حذف المهمة",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }

    assignmentPendingDeletion?.let { assignment ->
        ConfirmDeleteDialog(
            title = "حذف المهمة",
            message = "سيتم حذف المهمة «${assignment.title}» نهائيًا.",
            onDismiss = { assignmentPendingDeletion = null },
            onConfirm = {
                onDelete(assignment)
                assignmentPendingDeletion = null
            }
        )
    }
}

@Composable
fun CourseFilesTabContent(
    documents: List<com.example.data.model.LibraryDocumentEntity>,
    selectionMode: Boolean,
    selectedIds: List<Long>,
    onToggleSelection: (Long) -> Unit,
    onImportFile: () -> Unit,
    onOpenFile: (Long) -> Unit,
    onDelete: (com.example.data.model.LibraryDocumentEntity) -> Unit
) {
    var documentPendingDeletion by remember {
        mutableStateOf<com.example.data.model.LibraryDocumentEntity?>(null)
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ملفات وكتب المادة",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onImportFile, modifier = Modifier.testTag("import_course_file_button")) {
                    Icon(Icons.Default.Add, contentDescription = "إضافة ملف للمادة")
                }
            }
        }

        if (documents.isEmpty()) {
            item {
                Text(
                    text = "لا توجد ملفات مرتبطة بهذه المادة.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            }
        } else {
            items(documents) { doc ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (selectionMode) onToggleSelection(doc.id) else onOpenFile(doc.id)
                        },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selectionMode) {
                            BulkSelectionIndicator(doc.id in selectedIds)
                        }
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = JimPrimaryBlue,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = doc.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${doc.fileSize} • ${doc.fileType}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (!selectionMode) {
                            IconButton(onClick = { documentPendingDeletion = doc }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "حذف الملف",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    documentPendingDeletion?.let { document ->
        ConfirmDeleteDialog(
            title = "حذف الملف",
            message = "سيتم حذف «${document.title}» من مكتبة المادة.",
            onDismiss = { documentPendingDeletion = null },
            onConfirm = {
                onDelete(document)
                documentPendingDeletion = null
            }
        )
    }
}

@Composable
fun CourseInfoTabContent(course: CourseEntity) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "معلومات عامة", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(text = "المحاضر: ${course.instructor.ifEmpty { "غير محدد" }}")
                Text(text = "القاعة الدراسية: ${course.classroom.ifEmpty { "غير محددة" }}")
                Text(text = "الساعات المعتمدة: ${course.creditHours}")
                Text(text = "الفصل: ${course.semester}")
                if (course.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "وصف المقرر:", fontWeight = FontWeight.SemiBold)
                    Text(text = course.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// Dialogs
@Composable
fun AddAssessmentDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Double, Double, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var earned by remember { mutableStateOf("") }
    var max by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "إضافة تقييم جديد") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("اسم التقييم") }, modifier = Modifier.fillMaxWidth())
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = earned, onValueChange = { earned = it }, label = { Text("الدرجة المحرزة") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = max, onValueChange = { max = it }, label = { Text("الدرجة العظمى") }, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("التاريخ") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("ملاحظات") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val earnedVal = earned.toDoubleOrNull()
                    val maxVal = max.toDoubleOrNull()
                    if (earnedVal != null && maxVal != null && maxVal > 0) {
                        onConfirm(name, earnedVal, maxVal, 0.0, date, notes)
                    }
                },
                enabled = name.isNotBlank() &&
                    earned.toDoubleOrNull() != null &&
                    (max.toDoubleOrNull() ?: 0.0) > 0
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
fun AddLectureDialog(
    nextNumber: Int,
    onDismiss: () -> Unit,
    onConfirm: (String, Int, String, String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var num by remember { mutableIntStateOf(nextNumber) }
    var date by remember { mutableStateOf("") }
    var summary by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "تدوين محاضرة جديدة") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("عنوان المحاضرة") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = num.toString(), onValueChange = { num = it.toIntOrNull() ?: 1 }, label = { Text("رقم المحاضرة") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("تاريخ المحاضرة") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = summary, onValueChange = { summary = it }, label = { Text("ملخص المحتوى") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title, num, date, summary, "", "") },
                enabled = title.isNotBlank()
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
fun AddAssignmentDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("متوسط") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "إضافة واجب دراسي") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("اسم الواجب أو المشروع") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("التفاصيل والتعليمات") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = dueDate, onValueChange = { dueDate = it }, label = { Text("موعد التسليم") }, modifier = Modifier.fillMaxWidth())
                Text(text = "الأولوية:")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("عاجل", "متوسط", "منخفض").forEach { p ->
                        Button(
                            onClick = { priority = p },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = if (priority == p) JimPrimaryBlue else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text(p, color = if (priority == p) Color.White else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(title, desc, dueDate, priority) }, enabled = title.isNotBlank()) {
                Text("إضافة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
fun AddNoteDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("عام") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "ملاحظة جديدة للمقرر") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("العنوان") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("المحتوى") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(title, content, category) }, enabled = title.isNotBlank()) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
