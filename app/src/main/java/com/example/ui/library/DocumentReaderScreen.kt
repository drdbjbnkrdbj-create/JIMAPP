package com.example.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.JimViewModel
import com.example.ui.theme.JimNavyDark
import com.example.ui.theme.JimPrimaryBlue
import com.example.ui.theme.JimSkyAccent

enum class StudyTool {
    NONE,
    PEN,
    HIGHLIGHTER,
    ERASER
}

data class StrokeLine(
    val points: List<Offset>,
    val color: Color,
    val strokeWidth: Float,
    val isHighlighter: Boolean = false
)

@Composable
fun DocumentReaderScreen(
    docId: Long,
    viewModel: JimViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val documents by viewModel.documents.collectAsState()
    val doc = documents.find { it.id == docId } ?: return

    var currentPage by remember { mutableIntStateOf(doc.lastReadPage) }
    var zoomScale by remember { mutableFloatStateOf(1.0f) }

    // Study Mode Toolbar state
    var selectedTool by remember { mutableStateOf(StudyTool.NONE) }
    var selectedColor by remember { mutableStateOf(Color(0xFFFEF08A)) } // Yellow highlight default
    var showAddNoteDialog by remember { mutableStateOf(false) }

    // In-memory freehand drawing strokes
    val currentPoints = remember { mutableStateListOf<Offset>() }
    val drawnStrokes = remember { mutableStateListOf<StrokeLine>() }

    val annotations by viewModel.getAnnotationsForDoc(docId).collectAsState()
    val pageAnnotations = annotations.filter { it.page == currentPage }

    // Auto-save last read page when leaving or changing page
    DisposableEffect(currentPage) {
        viewModel.updateLastReadPage(doc, currentPage)
        onDispose {
            viewModel.updateLastReadPage(doc, currentPage)
        }
    }

    val paletteColors = listOf(
        Color(0xFFFEF08A) to "أصفر",
        Color(0xFFFED7AA) to "برتقالي",
        Color(0xFF7DD3FC) to "سماوي",
        Color(0xFFA7F3D0) to "أخضر",
        Color(0xFFDDD6FE) to "بنفسجي",
        Color(0xFFFECDD3) to "وردي"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("document_reader_screen")
    ) {
        // Reader Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("reader_back_button")) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "الرجوع",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = doc.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Zoom controls
                IconButton(onClick = { zoomScale = (zoomScale - 0.15f).coerceAtLeast(0.85f) }) {
                    Icon(imageVector = Icons.Default.ZoomOut, contentDescription = "تصغير", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { zoomScale = (zoomScale + 0.15f).coerceAtMost(1.5f) }) {
                    Icon(imageVector = Icons.Default.ZoomIn, contentDescription = "تكبير", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { viewModel.toggleDocBookmark(doc) }) {
                    Icon(
                        imageVector = if (doc.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "إشارة مرجعية",
                        tint = if (doc.isBookmarked) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Document Content View with Drawing & Highlighting Canvas Overlay
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            // Document Pages Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 60.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "${doc.title} - الصفحة $currentPage",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            fontSize = (18 * zoomScale).sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Render text with highlight styling
                        Text(
                            text = doc.contentSnippet.ifEmpty {
                                "محتوى المستند الأكاديمي:\n\nيحتوي هذا الملف على شروحات وتفاصيل المادة الدراسية، والتمارين المعتمدة للفصل الدراسي الحالي.\n\nيمكنك استخدام أدوات وضع الدراسة في الأسفل للرسم، تظليل النقاط المهمة، وإضافة الملاحظات الذكية."
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            fontSize = (15 * zoomScale).sp,
                            lineHeight = (24 * zoomScale).sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Render Saved Callout Note (as seen in screenshot)
                        pageAnnotations.forEach { annotation ->
                            Spacer(modifier = Modifier.height(14.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(JimSkyAccent.copy(alpha = 0.12f))
                                    .border(1.dp, JimSkyAccent.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .padding(14.dp)
                            ) {
                                Text(
                                    text = annotation.content,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontSize = (14 * zoomScale).sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Interactive Ink / Drawing Canvas Overlay when pen/highlighter is chosen
            if (selectedTool != StudyTool.NONE) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(selectedTool, selectedColor) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    if (selectedTool == StudyTool.ERASER) {
                                        drawnStrokes.clear()
                                    } else {
                                        currentPoints.clear()
                                        currentPoints.add(offset)
                                    }
                                },
                                onDrag = { change, _ ->
                                    if (selectedTool != StudyTool.ERASER) {
                                        currentPoints.add(change.position)
                                    }
                                },
                                onDragEnd = {
                                    if (selectedTool != StudyTool.ERASER && currentPoints.isNotEmpty()) {
                                        val isHigh = selectedTool == StudyTool.HIGHLIGHTER
                                        val width = if (isHigh) 28f else 6f
                                        val strokeColor = if (isHigh) selectedColor.copy(alpha = 0.45f) else selectedColor
                                        drawnStrokes.add(StrokeLine(currentPoints.toList(), strokeColor, width, isHigh))
                                        currentPoints.clear()
                                    }
                                }
                            )
                        }
                ) {
                    // Draw existing strokes
                    drawnStrokes.forEach { stroke ->
                        if (stroke.points.size > 1) {
                            val path = Path()
                            path.moveTo(stroke.points[0].x, stroke.points[0].y)
                            for (i in 1 until stroke.points.size) {
                                path.lineTo(stroke.points[i].x, stroke.points[i].y)
                            }
                            drawPath(
                                path = path,
                                color = stroke.color,
                                style = Stroke(
                                    width = stroke.strokeWidth,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }

                    // Draw active dragging stroke
                    if (currentPoints.size > 1) {
                        val path = Path()
                        path.moveTo(currentPoints[0].x, currentPoints[0].y)
                        for (i in 1 until currentPoints.size) {
                            path.lineTo(currentPoints[i].x, currentPoints[i].y)
                        }
                        val isHigh = selectedTool == StudyTool.HIGHLIGHTER
                        val width = if (isHigh) 28f else 6f
                        val strokeColor = if (isHigh) selectedColor.copy(alpha = 0.45f) else selectedColor
                        drawPath(
                            path = path,
                            color = strokeColor,
                            style = Stroke(
                                width = width,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }
            }
        }

        // Bottom Study Mode Toolbar (matching the Samsung Notes design in the screenshot)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)) {
                // Page Navigation Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { if (currentPage > 1) currentPage-- },
                        enabled = currentPage > 1
                    ) {
                        Text(text = "السابق", style = MaterialTheme.typography.labelSmall)
                    }

                    Text(
                        text = "$currentPage / ${doc.totalPages}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    IconButton(
                        onClick = { if (currentPage < doc.totalPages) currentPage++ },
                        enabled = currentPage < doc.totalPages
                    ) {
                        Text(text = "التالي", style = MaterialTheme.typography.labelSmall)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Tool selection and color dots (Pen, Highlighter, Eraser, Add Note, Colors)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tool buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        StudyToolButton(
                            icon = Icons.Default.Create,
                            isSelected = selectedTool == StudyTool.PEN,
                            onClick = { selectedTool = if (selectedTool == StudyTool.PEN) StudyTool.NONE else StudyTool.PEN }
                        )
                        StudyToolButton(
                            icon = Icons.Default.FormatColorFill,
                            isSelected = selectedTool == StudyTool.HIGHLIGHTER,
                            onClick = { selectedTool = if (selectedTool == StudyTool.HIGHLIGHTER) StudyTool.NONE else StudyTool.HIGHLIGHTER }
                        )
                        StudyToolButton(
                            icon = Icons.Default.Clear,
                            isSelected = selectedTool == StudyTool.ERASER,
                            onClick = {
                                selectedTool = StudyTool.ERASER
                                drawnStrokes.clear()
                            }
                        )
                        StudyToolButton(
                            icon = Icons.Default.AddComment,
                            isSelected = false,
                            onClick = { showAddNoteDialog = true }
                        )
                    }

                    // Color Palette Dots
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        paletteColors.forEach { (color, _) ->
                            val isChosen = selectedColor == color
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isChosen) 2.dp else 0.dp,
                                        color = if (isChosen) JimPrimaryBlue else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColor = color }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddNoteDialog) {
        var noteText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddNoteDialog = false },
            title = { Text("إضافة ملاحظة على الصفحة $currentPage") },
            text = {
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("نص الملاحظة") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (noteText.isNotBlank()) {
                            viewModel.addAnnotation(docId, currentPage, "NOTE", noteText, "#38BDF8")
                            showAddNoteDialog = false
                        }
                    }
                ) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddNoteDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun StudyToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) JimPrimaryBlue.copy(alpha = 0.2f) else Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) JimPrimaryBlue else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}
