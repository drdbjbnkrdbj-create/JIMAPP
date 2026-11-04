package com.example.ui.notes

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
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.example.data.model.CourseNoteEntity
import com.example.ui.JimViewModel
import com.example.ui.components.JimTopBar
import com.example.ui.components.BulkSelectionActions
import com.example.ui.components.BulkSelectionIndicator
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.library.shareText
import com.example.ui.theme.CourseAmber
import com.example.ui.theme.CourseRed
import com.example.ui.theme.JimPrimaryBlue

@Composable
fun NotesScreen(
    viewModel: JimViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val notes by viewModel.notes.collectAsState()
    val activeFilter by viewModel.notesFilter.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedNoteForEdit by remember { mutableStateOf<CourseNoteEntity?>(null) }
    val selectedNoteIds = remember { mutableStateListOf<Long>() }
    var selectionMode by remember { mutableStateOf(false) }
    var notesPendingDeletion by remember { mutableStateOf<List<CourseNoteEntity>>(emptyList()) }

    val categories = listOf("الكل", "عام", "مهم", "أسئلة", "مراجعة")

    val filteredNotes = notes.filter { note ->
        val matchesSearch = note.title.contains(searchQuery, ignoreCase = true) ||
                note.content.contains(searchQuery, ignoreCase = true)
        val matchesCategory = if (activeFilter == "الكل") true else note.category == activeFilter
        matchesSearch && matchesCategory
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = JimPrimaryBlue,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("add_quick_note_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "إضافة ملاحظة")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("quick_notes_screen")
        ) {
            JimTopBar(
                title = "الملاحظات السريعة",
                onBackClick = onBack,
                actions = {
                    IconButton(
                        onClick = {
                            selectionMode = !selectionMode
                            selectedNoteIds.clear()
                        }
                    ) {
                        Icon(
                            Icons.Default.Checklist,
                            contentDescription = "تحديد الملاحظات",
                            tint = if (selectionMode) JimPrimaryBlue else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )

            if (selectionMode) {
                BulkSelectionActions(
                    selectedCount = selectedNoteIds.size,
                    onSelectAll = {
                        selectedNoteIds.clear()
                        selectedNoteIds.addAll(filteredNotes.map { it.id })
                    },
                    onShare = {
                        val selectedNotes = notes.filter { it.id in selectedNoteIds }
                        shareText(
                            context,
                            "مشاركة الملاحظات المحددة",
                            selectedNotes.joinToString("\n\n") { "${it.title}\n${it.content}" }
                        )
                    },
                    onDelete = {
                        notesPendingDeletion = notes.filter { it.id in selectedNoteIds }
                    },
                    onCancel = {
                        selectedNoteIds.clear()
                        selectionMode = false
                    }
                )
            }

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                // Search Input matching screenshot ("ابحث في الملاحظات...")
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "ابحث في الملاحظات...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Category Chips: الكل, عام, مهم, أسئلة, مراجعة
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { cat ->
                        val isSelected = activeFilter == cat
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) JimPrimaryBlue else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { viewModel.setNotesFilter(cat) }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = cat,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Notes List
                if (filteredNotes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "لا توجد ملاحظات مدونة في هذا القسم.\nاضغط (+) لإضافة فكرة أو معلومة سريعة.",
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
                        items(filteredNotes) { note ->
                            NoteCardItem(
                                note = note,
                                selectionMode = selectionMode,
                                isSelected = note.id in selectedNoteIds,
                                onToggleSelected = {
                                    if (!selectedNoteIds.remove(note.id)) selectedNoteIds.add(note.id)
                                },
                                onClick = { if (!selectionMode) selectedNoteForEdit = note },
                                onTogglePin = { viewModel.togglePinNote(note) },
                                onDelete = { viewModel.deleteNote(note) }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
        }
    }

    notesPendingDeletion.takeIf { it.isNotEmpty() }?.let { selectedNotes ->
        ConfirmDeleteDialog(
            title = "حذف الملاحظات المحددة",
            message = "سيتم حذف ${selectedNotes.size} ملاحظات نهائيًا.",
            onDismiss = { notesPendingDeletion = emptyList() },
            onConfirm = {
                selectedNotes.forEach(viewModel::deleteNote)
                selectedNoteIds.clear()
                selectionMode = false
                notesPendingDeletion = emptyList()
            }
        )
    }

    if (showAddDialog) {
        AddOrEditNoteDialog(
            initialTitle = "",
            initialContent = "",
            initialCategory = "عام",
            initialPinned = true,
            onDismiss = { showAddDialog = false },
            onConfirm = { title, content, cat, pin ->
                viewModel.addNote(null, title, content, cat, pin)
                showAddDialog = false
            }
        )
    }

    selectedNoteForEdit?.let { note ->
        AddOrEditNoteDialog(
            initialTitle = note.title,
            initialContent = note.content,
            initialCategory = note.category,
            initialPinned = note.isPinned,
            onDismiss = { selectedNoteForEdit = null },
            onConfirm = { title, content, cat, pin ->
                viewModel.updateNote(note.copy(title = title, content = content, category = cat, isPinned = pin))
                selectedNoteForEdit = null
            }
        )
    }
}

@Composable
fun NoteCardItem(
    note: CourseNoteEntity,
    selectionMode: Boolean,
    isSelected: Boolean,
    onToggleSelected: () -> Unit,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { if (selectionMode) onToggleSelected() else onClick() }
            .testTag("note_item_${note.id}"),
        shape = RoundedCornerShape(16.dp),
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (selectionMode) {
                        BulkSelectionIndicator(isSelected)
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    if (note.isPinned) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "مثبتة",
                            tint = CourseRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (!selectionMode) Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onTogglePin, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "تثبيت",
                            tint = if (note.isPinned) CourseRed else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "حذف",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (note.content.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "التصنيف: ${note.category}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun AddOrEditNoteDialog(
    initialTitle: String,
    initialContent: String,
    initialCategory: String,
    initialPinned: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, Boolean) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var content by remember { mutableStateOf(initialContent) }
    var category by remember { mutableStateOf(initialCategory) }
    var isPinned by remember { mutableStateOf(initialPinned) }

    val categories = listOf("عام", "مهم", "أسئلة", "مراجعة")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialTitle.isEmpty()) "ملاحظة جديدة" else "تعديل الملاحظة", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("العنوان") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("المحتوى") },
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("التصنيف:", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    categories.forEach { cat ->
                        val isSel = category == cat
                        Button(
                            onClick = { category = cat },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = if (isSel) JimPrimaryBlue else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text(cat, color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface, fontSize = 11.sp)
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isPinned = !isPinned },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = null,
                        tint = if (isPinned) CourseRed else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "تثبيت الملاحظة في الأعلى", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title, content, category, isPinned) },
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
