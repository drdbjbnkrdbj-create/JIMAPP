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
        """(?<!\d)(\d{1,2})[:.٫：](\d{2})\s*(صباحاً|صباحا|صباح|مساءً|مساء|ص|م|AM|PM)?""",
        RegexOption.IGNORE_CASE
    )
    private val roomPattern = Regex(
        """(قاعة\s*[\p{L}\d-]+|معمل\s*\d+|Lab\d+|H\d+|C\d+)""",
        RegexOption.IGNORE_CASE
    )
    private val dayNames = listOf(
        listOf("الأحد", "الاحد", "Sunday") to 1,
        listOf("الإثنين", "الاثنين", "Monday") to 2,
        listOf("الثلاثاء", "Tuesday") to 3,
        listOf("الأربعاء", "الاربعاء", "Wednesday") to 4,
        listOf("الخميس", "Thursday") to 5
    )

    fun parse(text: String): List<ParsedSessionDraft> {
        val sessions = mutableListOf<ParsedSessionDraft>()
        var currentDay: Int? = null
        val pendingLines = mutableListOf<String>()

        for (rawLine in text.lines()) {
            val line = normalizeDigits(rawLine).trim()
            if (line.isBlank()) continue

            val explicitDay = findDay(line)
            if (explicitDay != null) {
                if (currentDay != null && currentDay != explicitDay) pendingLines.clear()
                currentDay = explicitDay
            }
            val day = currentDay ?: continue
            val combinedLine = (pendingLines + line).joinToString(" ")
            val timeMatches = timePattern.findAll(combinedLine).toList()

            if (timeMatches.size < 2) {
                pendingLines += line
                continue
            }

            pendingLines.clear()
            val times = timeMatches
                .mapNotNull { match -> parseTime(match.groupValues[1], match.groupValues[2], match.groupValues[3]) }
                .toList()

            if (times.size < 2) continue

            val roomMatch = roomPattern.find(combinedLine)?.value.orEmpty()
            var courseName = combinedLine
            dayNames.flatMap { it.first }.forEach { name ->
                courseName = courseName.replace(name, " ", ignoreCase = true)
            }
            timePattern.findAll(combinedLine).toList().asReversed().forEach { match ->
                courseName = courseName.removeRange(match.range)
            }
            if (roomMatch.isNotBlank()) {
                courseName = courseName.replace(roomMatch, " ", ignoreCase = true)
            }
            courseName = courseName
                .replace(Regex("""\b(?:من|إلى|الى|حتى|الساعة|AM|PM)\b""", RegexOption.IGNORE_CASE), " ")
                .replace("معمل", " ")
                .replace(Regex("""[:،,؛|\-–—]"""), " ")
                .replace(Regex("""\s+"""), " ")
                .trim()

            if (courseName.isBlank()) continue

            val isLab = combinedLine.contains("معمل", ignoreCase = true) ||
                combinedLine.contains("Lab", ignoreCase = true)
            sessions += ParsedSessionDraft(
                dayOfWeek = day,
                courseName = courseName,
                startTime = times[0],
                endTime = times[1],
                classroom = roomMatch,
                sessionType = if (isLab) "معمل" else "محاضرة"
            )
        }

        return sessions.sortedWith(compareBy(ParsedSessionDraft::dayOfWeek, ParsedSessionDraft::startTime))
    }

    fun hasValidTimeRange(startTime: String, endTime: String): Boolean {
        val start = parseClockTime(startTime) ?: return false
        val end = parseClockTime(endTime) ?: return false
        return end > start
    }

    private fun findDay(line: String): Int? =
        dayNames.firstNotNullOfOrNull { (names, day) ->
            if (names.any { name ->
                    if (name.first().isLatinLetter()) {
                        Regex("""(?i)(?<![\p{L}])${Regex.escape(name)}(?![\p{L}])""").containsMatchIn(line)
                    } else {
                        line.contains(name)
                    }
                }
            ) day else null
        }

    private fun parseTime(hourText: String, minuteText: String, meridiemText: String): String? {
        var hour = hourText.toIntOrNull() ?: return null
        val minute = minuteText.toIntOrNull() ?: return null
        val meridiem = meridiemText.lowercase()
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

    private fun Char.isLatinLetter(): Boolean = this in 'A'..'Z' || this in 'a'..'z'
}
