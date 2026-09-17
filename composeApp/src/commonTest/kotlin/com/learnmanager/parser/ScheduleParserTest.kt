package com.learnmanager.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScheduleParserTest {
    @Test
    fun parsesValidTsvAndHeader() {
        val input = """
            Day\tTime\tContent\tType
            T2\t08:30-10:00\tCyber Security Certificate\tTự học
            T3\t10:15-11:45\tNhập môn ATTT\tPreview
        """.trimIndent().replace("\\t", "\t")

        val result = ScheduleParser.parse(input)

        assertEquals(2, result.validRows.size)
        assertTrue(result.invalidRows.isEmpty())
        assertEquals(1, result.validRows.first().dayOfWeek)
        assertEquals("08:30", result.validRows.first().startTime)
    }

    @Test
    fun reportsMalformedRowsInsteadOfSilentlyDroppingThem() {
        val input = """
            T2\t10:00-09:00\tSai giờ\tPreview
            T9\t08:00-09:00\tSai thứ\tTự học
            T4\t08:00-09:00\tThiếu cột
        """.trimIndent().replace("\\t", "\t")

        val result = ScheduleParser.parse(input)

        assertTrue(result.validRows.isEmpty())
        assertEquals(3, result.invalidRows.size)
    }
}
