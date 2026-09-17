package com.learnmanager.parser

data class ScheduleDraft(
    val dayOfWeek: Int,
    val startTime: String,
    val endTime: String,
    val content: String,
    val type: String,
)

data class InvalidScheduleRow(
    val lineNumber: Int,
    val rawText: String,
    val reason: String,
)

data class ScheduleParseResult(
    val validRows: List<ScheduleDraft>,
    val invalidRows: List<InvalidScheduleRow>,
)

object ScheduleParser {
    private val timeRangeRegex = Regex(
        """^([01]?\d|2[0-3]):([0-5]\d)\s*[-–—]\s*([01]?\d|2[0-3]):([0-5]\d)$""",
    )

    fun parse(input: String): ScheduleParseResult {
        val valid = mutableListOf<ScheduleDraft>()
        val invalid = mutableListOf<InvalidScheduleRow>()

        input.lineSequence().forEachIndexed { index, originalLine ->
            val lineNumber = index + 1
            val line = originalLine.trim()
            if (line.isBlank()) return@forEachIndexed

            val columns = originalLine.split('\t').map(::normalizeWhitespace)
            if (lineNumber == firstNonBlankLineNumber(input) && looksLikeHeader(columns)) {
                return@forEachIndexed
            }

            if (columns.size != 4) {
                invalid += InvalidScheduleRow(
                    lineNumber = lineNumber,
                    rawText = originalLine,
                    reason = "Cần đúng 4 cột: Ngày, Giờ, Nội dung, Loại",
                )
                return@forEachIndexed
            }

            val day = parseDay(columns[0])
            if (day == null) {
                invalid += InvalidScheduleRow(lineNumber, originalLine, "Ngày không hợp lệ (dùng T2..T7 hoặc CN)")
                return@forEachIndexed
            }

            val time = parseTimeRange(columns[1])
            if (time == null) {
                invalid += InvalidScheduleRow(lineNumber, originalLine, "Giờ không hợp lệ (ví dụ 08:30-10:00)")
                return@forEachIndexed
            }

            if (columns[2].isBlank()) {
                invalid += InvalidScheduleRow(lineNumber, originalLine, "Nội dung không được để trống")
                return@forEachIndexed
            }

            if (columns[3].isBlank()) {
                invalid += InvalidScheduleRow(lineNumber, originalLine, "Loại lịch không được để trống")
                return@forEachIndexed
            }

            valid += ScheduleDraft(
                dayOfWeek = day,
                startTime = time.first,
                endTime = time.second,
                content = columns[2],
                type = columns[3],
            )
        }

        return ScheduleParseResult(validRows = valid, invalidRows = invalid)
    }

    fun parseDay(raw: String): Int? {
        val normalized = normalizeWhitespace(raw)
            .uppercase()
            .replace("Ứ", "U")
            .replace("Ừ", "U")
            .replace("Ư", "U")
            .replace(" ", "")

        return when (normalized) {
            "T2", "THU2", "MON", "MONDAY" -> 1
            "T3", "THU3", "TUE", "TUESDAY" -> 2
            "T4", "THU4", "WED", "WEDNESDAY" -> 3
            "T5", "THU5", "THU", "THURSDAY" -> 4
            "T6", "THU6", "FRI", "FRIDAY" -> 5
            "T7", "THU7", "SAT", "SATURDAY" -> 6
            "CN", "CHUNHAT", "SUN", "SUNDAY" -> 7
            else -> null
        }
    }

    fun parseTimeRange(raw: String): Pair<String, String>? {
        val match = timeRangeRegex.matchEntire(raw.trim()) ?: return null
        val startHour = match.groupValues[1].toInt()
        val startMinute = match.groupValues[2].toInt()
        val endHour = match.groupValues[3].toInt()
        val endMinute = match.groupValues[4].toInt()

        val startTotal = startHour * 60 + startMinute
        val endTotal = endHour * 60 + endMinute
        if (endTotal <= startTotal) return null

        return formatTime(startHour, startMinute) to formatTime(endHour, endMinute)
    }

    private fun looksLikeHeader(columns: List<String>): Boolean {
        if (columns.size != 4) return false
        val joined = columns.joinToString(" ").lowercase()
        return (joined.contains("day") || joined.contains("ngày") || joined.contains("thu") || joined.contains("thứ")) &&
            (joined.contains("time") || joined.contains("giờ"))
    }

    private fun firstNonBlankLineNumber(input: String): Int =
        input.lineSequence().indexOfFirst { it.isNotBlank() }.let { if (it < 0) -1 else it + 1 }

    private fun normalizeWhitespace(value: String): String =
        value.trim().replace(Regex("\\s+"), " ")

    private fun formatTime(hour: Int, minute: Int): String =
        hour.toString().padStart(2, '0') + ":" + minute.toString().padStart(2, '0')
}
