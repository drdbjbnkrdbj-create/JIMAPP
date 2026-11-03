package com.example.ui.schedule

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.TimetableSessionEntity
import com.example.ui.JimViewModel
import com.example.ui.components.JimTopBar

data class ParsedSessionDraft(
    var dayOfWeek: Int = 1,
    var courseName: String = "",
    var startTime: String = "08:00",
    var endTime: String = "09:00",
    var classroom: String = "",
    var sessionType: String = "محاضرة"
)

@Composable
fun ScheduleImportScreen(
    viewModel: JimViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val courses by viewModel.courses.collectAsState()
    var rawText by remember {
        mutableStateOf(
            """
الأحد: هندسة البرمجيات من 08:00 إلى 09:00 قاعة H2
الإثنين: الرياضيات المتقطعة 09:00 - 10:30 H1
الثلاثاء: معمل قواعد البيانات من 10:00 إلى 12:00 Lab6
الأربعاء: الذكاء الاصطناعي 09:00 - 11:00 H3
الخميس: لغة إنجليزية 11:00 - 01:00 C1
            """.trimIndent()
        )
    }

    val extractedList = remember { mutableStateListOf<ParsedSessionDraft>() }
    var parseError by remember { mutableStateOf<String?>(null) }
    var importSuccess by remember { mutableStateOf(false) }

    fun parseScheduleText(text: String) {
        extractedList.clear()
        parseError = null

        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        val dayMap = mapOf(
            "الأحد" to 1, "الاحد" to 1, "Sunday" to 1,
            "الإثنين" to 2, "الاثنين" to 2, "Monday" to 2,
            "الثلاثاء" to 3, "Tuesday" to 3,
            "الأربعاء" to 4, "الاربعاء" to 4, "Wednesday" to 4,
            "الخميس" to 5, "Thursday" to 5
        )

        for (line in lines) {
            var foundDay = 1
            for ((key, value) in dayMap) {
                if (line.contains(key)) {
                    foundDay = value
                    break
                }
            }

            // Extract times like 08:00, 10:30
            val timeRegex = Regex("""(\d{1,2}:\d{2})""")
            val timeMatches = timeRegex.findAll(line).toList()
            val startTime = if (timeMatches.isNotEmpty()) timeMatches[0].value else "08:00"
            val endTime = if (timeMatches.size > 1) timeMatches[1].value else "09:30"

            // Extract classroom if available (e.g. H1, H2, Lab6, C1, قاعة 2)
            val roomRegex = Regex("""(H\d+|Lab\d+|C\d+|قاعة\s*\w+|معمل\s*\w+)""", RegexOption.IGNORE_CASE)
            val roomMatch = roomRegex.find(line)?.value ?: ""

            // Extract session type
            val type = if (line.contains("معمل") || line.contains("Lab", ignoreCase = true)) "معمل" else "محاضرة"

            // Extract course name by removing day, time, room keywords
            var cleanName = line
            dayMap.keys.forEach { cleanName = cleanName.replace(it, "") }
            cleanName = cleanName.replace(Regex("""[:\-منإلى,]"""), " ")
            timeMatches.forEach { cleanName = cleanName.replace(it.value, "") }
            if (roomMatch.isNotBlank()) cleanName = cleanName.replace(roomMatch, "")
            cleanName = cleanName.replace("قاعة", "").replace("معمل", "").trim()

            if (cleanName.isBlank()) {
                cleanName = "محاضرة جامعية"
            }

            extractedList.add(
                ParsedSessionDraft(
                    dayOfWeek = foundDay,
                    courseName = cleanName,
                    startTime = startTime,
                    endTime = endTime,
                    classroom = roomMatch,
                    sessionType = type
                )
            )
        }

        if (extractedList.isEmpty()) {
            parseError = "تعذر استخراج الحصص تلقائياً من النص. يرجى التأكد من كتابة اليوم وأوقات الحصص."
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("schedule_import_screen")
    ) {
        JimTopBar(
            title = "استيراد الجدول الدراسي",
            onBackClick = onBack
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "أداة الاستيراد الذكي للجدول (دون إنترنت)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "الصق نص جدولك الجامعي من ملف أو رسالة، وسيقوم النظام باستخراج الأيام والأوقات والقاعات لمراجعتها وتعديلها قبل الاعتماد.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = rawText,
                    onValueChange = { rawText = it },
                    label = { Text("الصق نص الجدول هنا") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("schedule_raw_text_input"),
                    minLines = 4,
                    maxLines = 8
                )
            }

            item {
                Button(
                    onClick = { parseScheduleText(rawText) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("parse_schedule_button")
                ) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("استخراج الحصص وتجهيز المراجعة")
                }
            }

            parseError?.let { err ->
                item {
                    Text(text = err, color = MaterialTheme.colorScheme.error)
                }
            }

            if (extractedList.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "مراجعة الحصص المستخرجة (${extractedList.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                itemsIndexed(extractedList) { index, draft ->
                    val dayNames = listOf("الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس")
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "حصة ${index + 1} - ${dayNames.getOrElse(draft.dayOfWeek - 1) { "الأحد" }}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                IconButton(onClick = { extractedList.removeAt(index) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                                }
                            }

                            OutlinedTextField(
                                value = draft.courseName,
                                onValueChange = { draft.courseName = it },
                                label = { Text("اسم المادة") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = draft.startTime,
                                    onValueChange = { draft.startTime = it },
                                    label = { Text("البداية") },
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = draft.endTime,
                                    onValueChange = { draft.endTime = it },
                                    label = { Text("النهاية") },
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = draft.classroom,
                                    onValueChange = { draft.classroom = it },
                                    label = { Text("القاعة") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                item {
                    Button(
                        onClick = {
                            val newSessions = extractedList.map { draft ->
                                val matchedCourse = courses.find {
                                    it.name.contains(draft.courseName, ignoreCase = true) ||
                                    draft.courseName.contains(it.name, ignoreCase = true)
                                } ?: courses.firstOrNull()

                                TimetableSessionEntity(
                                    courseId = matchedCourse?.id ?: 1L,
                                    dayOfWeek = draft.dayOfWeek,
                                    startTime = draft.startTime,
                                    endTime = draft.endTime,
                                    classroom = draft.classroom,
                                    sessionType = draft.sessionType
                                )
                            }

                            viewModel.importParsedSessions(newSessions)
                            importSuccess = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("confirm_import_button")
                    ) {
                        Icon(imageVector = Icons.Default.Upload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تأكيد الاستيراد إلى الجدول الأسبوعي")
                    }
                }
            }

            if (importSuccess) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "تم استيراد الجدول بنجاح! يمكنك الآن الرجوع للجدول واستعراض الحصص.",
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = onBack) {
                                Text("العودة إلى الجدول")
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
