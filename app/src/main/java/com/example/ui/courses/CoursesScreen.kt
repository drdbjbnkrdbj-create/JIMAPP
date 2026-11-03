package com.example.ui.courses

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.CourseEntity
import com.example.ui.JimViewModel
import com.example.ui.components.CourseIconBadge
import com.example.ui.theme.JimPrimaryBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoursesScreen(
    viewModel: JimViewModel,
    onOpenCourse: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val courses by viewModel.courses.collectAsState()
    val searchQuery by viewModel.courseSearchQuery.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    val filteredCourses = courses.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
        it.code.contains(searchQuery, ignoreCase = true) ||
        it.instructor.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("courses_screen")
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Top bar with Title and Add (+) button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .testTag("add_course_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "إضافة مادة",
                    tint = JimPrimaryBlue
                )
            }

            Text(
                text = "المواد الدراسية",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            // Balance spacer
            Spacer(modifier = Modifier.size(40.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search Field matching screenshot ("إبحث عن مادة...")
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setCourseSearch(it) },
            placeholder = {
                Text(
                    text = "إبحث عن مادة...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "بحث",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("course_search_input")
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Courses List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredCourses) { course ->
                CourseItemCard(
                    course = course,
                    onClick = { onOpenCourse(course.id) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showAddDialog) {
        AddCourseDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, code, instructor, classroom, credits, color, icon, desc ->
                viewModel.addCourse(name, code, instructor, classroom, credits, color, icon, desc)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun CourseItemCard(
    course: CourseEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("course_item_${course.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CourseIconBadge(
                iconKey = course.iconKey,
                colorHex = course.colorHex,
                size = 50.dp
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = course.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${course.code} | ${course.creditHours} ساعات معتمدة",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun AddCourseDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, Int, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var instructor by remember { mutableStateOf("") }
    var classroom by remember { mutableStateOf("") }
    var credits by remember { mutableIntStateOf(3) }
    var selectedColor by remember { mutableStateOf("#2563EB") }
    var selectedIcon by remember { mutableStateOf("code") }
    var description by remember { mutableStateOf("") }

    val colorOptions = listOf("#2563EB", "#10B981", "#F59E0B", "#8B5CF6", "#EF4444", "#06B6D4")
    val iconOptions = listOf(
        "code" to "برمجة",
        "math" to "رياضيات",
        "database" to "قواعد بيانات",
        "ai" to "ذكاء اصطناعي",
        "language" to "لغات"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "إضافة مادة جديدة", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم المادة") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("رمز المادة (مثال: CS301)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = instructor,
                    onValueChange = { instructor = it },
                    label = { Text("أستاذ المادة") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = classroom,
                        onValueChange = { classroom = it },
                        label = { Text("القاعة / المعمل") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = credits.toString(),
                        onValueChange = { credits = it.toIntOrNull() ?: 3 },
                        label = { Text("الساعات") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(text = "اختر لون المادة:", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    colorOptions.forEach { hex ->
                        val isSelected = selectedColor == hex
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(hex)))
                                .clickable { selectedColor = hex }
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                        .align(Alignment.Center)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && code.isNotBlank()) {
                        onConfirm(name, code, instructor, classroom, credits, selectedColor, selectedIcon, description)
                    }
                },
                enabled = name.isNotBlank() && code.isNotBlank()
            ) {
                Text("إضافة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
