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
    private val spaceColumnSeparatorRegex = Regex(""" {2,}""")
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

            val columns = splitColumns(originalLine)
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

    /**
     * Rewrites recognizable rows to the display/import format used by the app:
     * T2  08:30-10:00  Nội dung  Loại
     *
     * Rows that still cannot be recognized are kept so the preview can report
     * their original line number and explain what must be corrected manually.
     */
    fun format(input: String): String = input.lineSequence().joinToString("\n") { originalLine ->
        if (originalLine.isBlank()) return@joinToString ""

        val columns = splitColumns(originalLine)
        if (columns.size != 4 || looksLikeHeader(columns)) {
            return@joinToString originalLine.trim()
        }

        val day = parseDay(columns[0])
        val time = parseTimeRange(columns[1])
        if (day == null || time == null || columns[2].isBlank() || columns[3].isBlank()) {
            originalLine.trim()
        } else {
            listOf(dayCode(day), "${time.first}-${time.second}", columns[2], columns[3])
                .joinToString("  ")
        }
    }

    fun parseDay(raw: String): Int? {
        val normalized = buildString {
            for (ch in normalizeWhitespace(raw).uppercase()) {
                append(diacriticFold[ch] ?: ch)
            }
        }.filterNot { it == ' ' }

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

    /** Folds Vietnamese diacritics to base letters so "Thứ 4", "CHỦ NHẬT" match their ASCII keys. */
    private val diacriticFold = mapOf(
        'Đ' to 'D',
        'Á' to 'A', 'À' to 'A', 'Ả' to 'A', 'Ã' to 'A', 'Ạ' to 'A',
        'Ă' to 'A', 'Ắ' to 'A', 'Ằ' to 'A', 'Ẳ' to 'A', 'Ẵ' to 'A', 'Ặ' to 'A',
        'Â' to 'A', 'Ấ' to 'A', 'Ầ' to 'A', 'Ẩ' to 'A', 'Ẫ' to 'A', 'Ậ' to 'A',
        'É' to 'E', 'È' to 'E', 'Ẻ' to 'E', 'Ẽ' to 'E', 'Ẹ' to 'E',
        'Ê' to 'E', 'Ế' to 'E', 'Ề' to 'E', 'Ể' to 'E', 'Ễ' to 'E', 'Ệ' to 'E',
        'Í' to 'I', 'Ì' to 'I', 'Ỉ' to 'I', 'Ĩ' to 'I', 'Ị' to 'I',
        'Ó' to 'O', 'Ò' to 'O', 'Ỏ' to 'O', 'Õ' to 'O', 'Ọ' to 'O',
        'Ô' to 'O', 'Ố' to 'O', 'Ồ' to 'O', 'Ổ' to 'O', 'Ỗ' to 'O', 'Ộ' to 'O',
        'Ơ' to 'O', 'Ớ' to 'O', 'Ờ' to 'O', 'Ở' to 'O', 'Ỡ' to 'O', 'Ợ' to 'O',
        'Ú' to 'U', 'Ù' to 'U', 'Ủ' to 'U', 'Ũ' to 'U', 'Ụ' to 'U',
        'Ư' to 'U', 'Ứ' to 'U', 'Ừ' to 'U', 'Ử' to 'U', 'Ữ' to 'U', 'Ự' to 'U',
        'Ý' to 'Y', 'Ỳ' to 'Y', 'Ỷ' to 'Y', 'Ỹ' to 'Y', 'Ỵ' to 'Y',
    )

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

    private fun splitColumns(line: String): List<String> {
        val trimmed = line.trim()
        val columns = if ('\t' in trimmed) {
            trimmed.split('\t', limit = 4)
        } else {
            trimmed.split(spaceColumnSeparatorRegex, limit = 4)
        }
        return columns.map(::normalizeWhitespace)
    }

    private fun dayCode(dayOfWeek: Int): String = if (dayOfWeek == 7) "CN" else "T${dayOfWeek + 1}"

    private fun normalizeWhitespace(value: String): String =
        value.trim().replace(Regex("\\s+"), " ")

    private fun formatTime(hour: Int, minute: Int): String =
        hour.toString().padStart(2, '0') + ":" + minute.toString().padStart(2, '0')
}
