package com.learnmanager.time

import com.learnmanager.model.ScheduleEntry
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScheduleTimeTest {
    private val weekly = ScheduleEntry(
        id = "test",
        dayOfWeek = 1,
        startTime = "08:30",
        endTime = "10:00",
        content = "Test",
        type = "Preview",
        weekStartDate = "2026-09-21",
        timeZoneId = "Asia/Ho_Chi_Minh",
        repeatWeekly = true,
    )

    @Test
    fun weeklyEntryOccursOnFutureMondays() {
        assertTrue(occursOn(weekly, LocalDate.parse("2026-09-21")))
        assertTrue(occursOn(weekly, LocalDate.parse("2026-09-28")))
        assertFalse(occursOn(weekly, LocalDate.parse("2026-09-22")))
        assertFalse(occursOn(weekly, LocalDate.parse("2026-09-14")))
    }
}
