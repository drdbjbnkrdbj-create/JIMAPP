package com.example.ui.library

import android.content.Intent
import android.content.ActivityNotFoundException
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LibraryDocumentEntity
import com.example.ui.JimViewModel
import com.example.ui.theme.CourseAmber
import com.example.ui.theme.CourseBlue
import com.example.ui.theme.CourseCyan
import com.example.ui.theme.CourseRed
import com.example.ui.theme.JimPrimaryBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: JimViewModel,
    onOpenDoc: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val documents by viewModel.documents.collectAsState()
    val searchQuery by viewModel.librarySearchQuery.collectAsState()
    val activeFilter by viewModel.libraryFilter.collectAsState()

    var showAddFileDialog by remember { mutableStateOf(false) }
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val hasPersistentAccess = try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                true
            } catch (error: SecurityException) {
                Toast.makeText(context, "تعذر الاحتفاظ بإذن قراءة الملف.", Toast.LENGTH_LONG).show()
                false
            }

            if (hasPersistentAccess) {
                var fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "مستند"
                var fileSizeBytes: Long? = null
                context.contentResolver.query(
                    uri,
                    arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameColumn = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeColumn = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (nameColumn >= 0) fileName = cursor.getString(nameColumn) ?: fileName
                        if (sizeColumn >= 0 && !cursor.isNull(sizeColumn)) {
                            fileSizeBytes = cursor.getLong(sizeColumn)
                        }
                    }
                }

                val mimeType = context.contentResolver.getType(uri)
                    ?: MimeTypeMap.getSingleton()
                        .getMimeTypeFromExtension(fileName.substringAfterLast('.', "").lowercase())
                    ?: "*/*"
                val extension = fileName.substringAfterLast('.', "").lowercase()
                val fileType = when {
                    mimeType == "application/pdf" || extension == "pdf" -> "PDF"
                    extension in setOf("doc", "docx", "odt") ||
                        mimeType.contains("word", ignoreCase = true) -> "WORD"
                    extension in setOf("ppt", "pptx", "odp") ||
                        mimeType.contains("presentation", ignoreCase = true) ||
                        mimeType.contains("powerpoint", ignoreCase = true) -> "PPTX"
                    mimeType.startsWith("image/") -> "IMAGE"
                    else -> "FILE"
                }
                val title = fileName.substringBeforeLast('.', fileName).ifBlank { fileName }
                viewModel.addDocument(
                    courseId = null,
                    title = title,
                    fileType = fileType,
                    fileSize = formatFileSize(fileSizeBytes),
                    category = "محاضرات",
                    contentSnippet = "",
                    uri = uri.toString()
                )
            }
        }
    }

    val filterChips = listOf("الكل", "PDF", "Word", "PowerPoint")

    val filteredDocs = documents.filter { doc ->
        val matchesSearch = doc.title.contains(searchQuery, ignoreCase = true) ||
                doc.category.contains(searchQuery, ignoreCase = true)

        val matchesFilter = when (activeFilter) {
            "PDF" -> doc.fileType == "PDF"
            "Word" -> doc.fileType == "WORD"
            "PowerPoint" -> doc.fileType == "PPTX"
            else -> true
        }

        matchesSearch && matchesFilter
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { filePicker.launch(arrayOf("*/*")) },
                containerColor = JimPrimaryBlue,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("add_document_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "استيراد ملف")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .testTag("library_screen")
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Top Header: Title & Camera scan button matching screenshot
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { showAddFileDialog = true },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "مسح مستند",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "المكتبة الجامعية",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.size(40.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Search Bar matching screenshot ("ابحث عن ملف أو مادة...")
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setLibrarySearch(it) },
                placeholder = {
                    Text(
                        text = "ابحث عن ملف أو مادة...",
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
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("library_search_input")
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Category Filter Pills: الكل, PDF, Word, PowerPoint
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(filterChips) { chip ->
                    val isSelected = activeFilter == chip
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) JimPrimaryBlue else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { viewModel.setLibraryFilter(chip) }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .testTag("library_filter_$chip"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = chip,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Document list matching the design mock
            if (filteredDocs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا توجد مستندات في هذا التصنيف.\nاضغط (+) لإضافة وتصفح مراجعك ومحاضراتك.",
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
                    items(filteredDocs) { doc ->
                        LibraryDocumentCard(
                            doc = doc,
                            onClick = {
                                val documentUri = doc.uri
                                if (documentUri == null) {
                                    onOpenDoc(doc.id)
                                } else {
                                    val fileUri = Uri.parse(documentUri)
                                    val mimeType = context.contentResolver.getType(fileUri)
                                        ?: "*/*"
                                    val openIntent = Intent(Intent.ACTION_VIEW)
                                        .setDataAndType(fileUri, mimeType)
                                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    try {
                                        context.startActivity(
                                            Intent.createChooser(openIntent, "فتح ${doc.title}")
                                        )
                                    } catch (error: ActivityNotFoundException) {
                                        Toast.makeText(
                                            context,
                                            "لا يوجد تطبيق لفتح هذا النوع من الملفات.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            },
                            onShare = {
                                val documentUri = doc.uri
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    if (documentUri == null) {
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "مستند دراسي من تطبيق جيم: ${doc.title}\n\n${doc.contentSnippet.take(200)}"
                                        )
                                        type = "text/plain"
                                    } else {
                                        val fileUri = Uri.parse(documentUri)
                                        putExtra(Intent.EXTRA_STREAM, fileUri)
                                        type = context.contentResolver.getType(fileUri) ?: "*/*"
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "مشاركة المستند")
                                context.startActivity(shareIntent)
                            },
                            onBookmarkToggle = { viewModel.toggleDocBookmark(doc) }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    if (showAddFileDialog) {
        AddDocumentDialog(
            onDismiss = { showAddFileDialog = false },
            onConfirm = { title, type, size, category, snippet ->
                viewModel.addDocument(null, title, type, size, category, snippet)
                showAddFileDialog = false
            }
        )
    }
}

private fun formatFileSize(bytes: Long?): String {
    if (bytes == null || bytes < 0L) return "الحجم غير معروف"
    if (bytes < 1024L) return "$bytes B"
    val kilobytes = bytes / 1024.0
    if (kilobytes < 1024.0) return "%.1f KB".format(kilobytes)
    return "%.1f MB".format(kilobytes / 1024.0)
}

@Composable
fun LibraryDocumentCard(
    doc: LibraryDocumentEntity,
    onClick: () -> Unit,
    onShare: () -> Unit,
    onBookmarkToggle: () -> Unit
) {
    val (badgeBg, icon) = when (doc.fileType.uppercase()) {
        "PDF" -> CourseRed to Icons.Default.PictureAsPdf
        "WORD" -> CourseBlue to Icons.Default.Description
        "PPTX" -> CourseAmber to Icons.Default.Slideshow
        else -> CourseCyan to Icons.Default.Image
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("document_card_${doc.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Badge with filetype color
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(badgeBg.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = doc.fileType,
                    tint = badgeBg,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = doc.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${doc.fileSize} • ${doc.fileType}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onShare, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "مشاركة",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(onClick = onBookmarkToggle, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = if (doc.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "إشارة مرجعية",
                        tint = if (doc.isBookmarked) CourseAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun AddDocumentDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var fileType by remember { mutableStateOf("PDF") }
    var category by remember { mutableStateOf("الكل") }
    var snippet by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "إضافة مستند أو مرجع دراسي", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان الملف") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text(text = "نوع المستند:")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("PDF", "WORD", "PPTX", "IMAGE").forEach { type ->
                        val isSel = fileType == type
                        Button(
                            onClick = { fileType = type },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = if (isSel) JimPrimaryBlue else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text(type, color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface, fontSize = 11.sp)
                        }
                    }
                }

                OutlinedTextField(
                    value = snippet,
                    onValueChange = { snippet = it },
                    label = { Text("المحتوى أو الملاحظات الدراسية المرفقة") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title, fileType, "2.0 MB", category, snippet) },
                enabled = title.isNotBlank()
            ) {
                Text("إضافة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
