package com.example.ui.schedule

import java.util.Locale

data class ParsedSessionDraft(
    val dayOfWeek: Int = 1,
    val courseName: String = "",
    val startTime: String = "08:00",
    val endTime: String = "09:00",
    val classroom: String = "",
    val sessionType: String = "محاضرة"
)

object ScheduleTextParser {
    private val timePattern = Regex(
        """(?<!\p{Nd})(\p{Nd}{1,2})[:.٫：](\p{Nd}{2})\s*(صباحاً|صباحا|صباح|مساءً|مساء|ص|م|AM|PM)?""",
        RegexOption.IGNORE_CASE
    )
    private val roomPattern = Regex(
        listOf(
            """قاعة\s*(?:رقم\s*)?[\p{L}\p{Nd}-]+""",
            """(?:معمل|مختبر)\s*(?:رقم\s*)?(?:Lab\s*)?[\p{L}]*\p{Nd}+""",
            """Lab\s*[\p{L}\p{Nd}-]+""",
            """[HC]\s*-?\s*\p{Nd}+"""
        ).joinToString("|"),
        RegexOption.IGNORE_CASE
    )
    private val dayNames = listOf(
        listOf("الأحد", "الاحد", "Sunday") to 1,
        listOf("الإثنين", "الأثنين", "الاثنين", "Monday") to 2,
        listOf("الثلاثاء", "Tuesday") to 3,
        listOf("الأربعاء", "الاربعاء", "Wednesday") to 4,
        listOf("الخميس", "Thursday") to 5
    )

    fun parse(text: String): List<ParsedSessionDraft> {
        val sessions = mutableListOf<ParsedSessionDraft>()
        var currentDay: Int? = null
        var pendingText = ""

        for (rawLine in text.lines()) {
            val line = rawLine.trim()
            if (line.isBlank()) continue

            val explicitDay = findDay(line)
            if (explicitDay != null) {
                if (currentDay != null && currentDay != explicitDay) pendingText = ""
                currentDay = explicitDay
            }

            val day = currentDay ?: continue
            val lineWithoutDay = removeDayNames(line).trim()
            if (lineWithoutDay.isBlank()) continue

            val combinedText = listOf(pendingText, lineWithoutDay)
                .filter(String::isNotBlank)
                .joinToString(" ")
            val timeMatches = timePattern.findAll(combinedText).toList()

            if (timeMatches.size < 2) {
                pendingText = combinedText
                continue
            }

            val completePairs = timeMatches.size / 2
            repeat(completePairs) { pairIndex ->
                val startMatch = timeMatches[pairIndex * 2]
                val endMatch = timeMatches[pairIndex * 2 + 1]
                val segmentStart = if (pairIndex == 0) 0 else timeMatches[pairIndex * 2 - 1].range.last + 1
                val segmentEnd = if (pairIndex + 1 < completePairs) {
                    timeMatches[(pairIndex + 1) * 2].range.first
                } else {
                    combinedText.length
                }
                val sessionText = combinedText.substring(segmentStart, segmentEnd)
                val courseBeforeTime = combinedText.substring(segmentStart, startMatch.range.first)
                val courseAfterTime = combinedText.substring(endMatch.range.last + 1, segmentEnd)
                val startTime = parseTime(
                    startMatch.groupValues[1],
                    startMatch.groupValues[2],
                    startMatch.groupValues[3]
                )
                val endTime = parseTime(
                    endMatch.groupValues[1],
                    endMatch.groupValues[2],
                    endMatch.groupValues[3]
                )
                val roomMatches = roomPattern.findAll(sessionText).toList()
                val courseName = extractCourseName(courseBeforeTime)
                    .ifBlank { extractCourseName(courseAfterTime) }

                if (
                    startTime != null &&
                    endTime != null &&
                    hasValidTimeRange(startTime, endTime) &&
                    courseName.isNotBlank()
                ) {
                    val classroom = roomMatches.lastOrNull()?.value.orEmpty()
                        .replace(Regex("""^(?:معمل|مختبر)\s+(?=Lab)""", RegexOption.IGNORE_CASE), "")
                    val isLab = Regex("""(?<![\p{L}])(?:معمل|مختبر)(?![\p{L}])""")
                        .containsMatchIn(sessionText) ||
                        classroom.contains("معمل", ignoreCase = true) ||
                        classroom.contains("مختبر", ignoreCase = true) ||
                        classroom.contains("Lab", ignoreCase = true)
                    sessions += ParsedSessionDraft(
                        dayOfWeek = day,
                        courseName = courseName,
                        startTime = startTime,
                        endTime = endTime,
                        classroom = classroom,
                        sessionType = if (isLab) "معمل" else "محاضرة"
                    )
                }
            }

            pendingText = if (timeMatches.size % 2 == 1) {
                combinedText.substring(timeMatches[completePairs * 2 - 1].range.last + 1)
            } else {
                ""
            }
        }

        return sessions.sortedWith(compareBy(ParsedSessionDraft::dayOfWeek, ParsedSessionDraft::startTime))
    }

    fun hasValidTimeRange(startTime: String, endTime: String): Boolean {
        val start = parseClockTime(startTime) ?: return false
        val end = parseClockTime(endTime) ?: return false
        return end > start
    }

    fun formatTimeForDisplay(time24: String): String? {
        val minutes = parseClockTime(time24) ?: return null
        val hour = minutes / 60
        val minute = minutes % 60
        val displayHour = when (hour % 12) {
            0 -> 12
            else -> hour % 12
        }
        val period = if (hour < 12) "ص" else "م"
        return String.format(Locale.ROOT, "%d:%02d %s", displayHour, minute, period)
    }

    private fun findDay(line: String): Int? =
        dayNames.firstNotNullOfOrNull { (names, day) ->
            if (names.any { name ->
                    Regex(
                        """^\s*${Regex.escape(name)}(?=$|[\s:：،,;|–—-])""",
                        RegexOption.IGNORE_CASE
                    ).containsMatchIn(line)
                }
            ) {
                day
            } else {
                null
            }
        }

    private fun removeDayNames(line: String): String =
        dayNames
            .flatMap { it.first }
            .firstNotNullOfOrNull { name ->
                Regex(
                    """^\s*${Regex.escape(name)}(?=$|[\s:：،,;|–—-])""",
                    RegexOption.IGNORE_CASE
                ).find(line)
            }
            ?.let { match -> line.removeRange(match.range).trimStart(' ', ':', '：', '،', ',', ';', '|', '–', '—', '-') }
            ?: line

    private fun extractCourseName(sessionText: String): String {
        var courseName = timePattern.replace(sessionText, " ")
        roomPattern.findAll(sessionText).toList().asReversed().forEach { match ->
            courseName = courseName.removeRange(match.range)
        }

        return courseName
            .replace(Regex("""^\s*(?:(?:المادة|مادة|اليوم|يوم)\s*[:：-]?\s*)+"""), "")
            .replace(Regex("""^\s*(?:معمل|مختبر)\s+"""), "")
            .replace(Regex("""\s+(?:من|إلى|الى|حتى|الساعة)\s*$"""), "")
            .replace(Regex("""^[\s:：،,؛|–—-]+|[\s:：،,؛|–—-]+$"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    private fun parseTime(hourText: String, minuteText: String, meridiemText: String): String? {
        var hour = normalizeDigits(hourText).toIntOrNull() ?: return null
        val minute = normalizeDigits(minuteText).toIntOrNull() ?: return null
        val meridiem = meridiemText.lowercase(Locale.ROOT)
        val isPm = meridiem == "pm" || meridiem == "م" || meridiem.startsWith("مساء")
        val isAm = meridiem == "am" || meridiem == "ص" || meridiem.startsWith("صباح")

        if (minute !in 0..59) return null
        if (isAm || isPm) {
            if (hour !in 1..12) return null
            hour %= 12
            if (isPm) hour += 12
        } else if (hour !in 0..23) {
            return null
        }

        return String.format(Locale.ROOT, "%02d:%02d", hour, minute)
    }

    private fun parseClockTime(value: String): Int? {
        val match = Regex("""^([01]\d|2[0-3]):([0-5]\d)$""").matchEntire(value) ?: return null
        return match.groupValues[1].toInt() * 60 + match.groupValues[2].toInt()
    }

    private fun normalizeDigits(value: String): String = buildString(value.length) {
        value.forEach { char ->
            append(
                when (char) {
                    in '\u0660'..'\u0669' -> '0' + (char - '\u0660')
                    in '\u06F0'..'\u06F9' -> '0' + (char - '\u06F0')
                    else -> char
                }
            )
        }
    }
}
