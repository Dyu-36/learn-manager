package com.learnmanager.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScheduleParserTest {
    @Test
    fun parsesRowsSeparatedByDoubleSpaces() {
        val result = ScheduleParser.parse(
            "T2  08:30-10:00  Cyber Security Certificate  Tự học",
        )

        assertTrue(result.invalidRows.isEmpty())
        assertEquals(
            ScheduleDraft(1, "08:30", "10:00", "Cyber Security Certificate", "Tự học"),
            result.validRows.single(),
        )
    }

    @Test
    fun keepsSupportingPastedTabSeparatedRowsAndHeader() {
        val input = """
            Day\tTime\tContent\tType
            T3\t10:15-11:45\tNhập môn ATTT\tPreview
        """.trimIndent().replace("\\t", "\t")

        val result = ScheduleParser.parse(input)

        assertEquals(1, result.validRows.size)
        assertTrue(result.invalidRows.isEmpty())
    }

    @Test
    fun formatsRecognizableInputBeforeImport() {
        val input = "T2\t8:30 – 10:00\tCyber   Security Certificate\tTự   học"

        val formatted = ScheduleParser.format(input)

        assertEquals("T2  08:30-10:00  Cyber Security Certificate  Tự học", formatted)
        assertEquals(1, ScheduleParser.parse(formatted).validRows.size)
    }

    @Test
    fun reportsMalformedRowsInsteadOfSilentlyDroppingThem() {
        val input = """
            T2  10:00-09:00  Sai giờ  Preview
            T9  08:00-09:00  Sai thứ  Tự học
            T4  08:00-09:00  Thiếu cột
        """.trimIndent()

        val result = ScheduleParser.parse(input)

        assertTrue(result.validRows.isEmpty())
        assertEquals(3, result.invalidRows.size)
    }
}
