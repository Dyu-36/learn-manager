package com.learnmanager.time

import com.learnmanager.model.ScheduleEntry
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

fun currentWeekStart(timeZone: TimeZone = TimeZone.currentSystemDefault()): LocalDate {
    val today = Clock.System.now().toLocalDateTime(timeZone).date
    val offset = today.dayOfWeek.ordinal
    return today.plus(DatePeriod(days = -offset))
}

fun baseOccurrenceDate(entry: ScheduleEntry): LocalDate =
    LocalDate.parse(entry.weekStartDate).plus(DatePeriod(days = entry.dayOfWeek - 1))

fun occursOn(entry: ScheduleEntry, date: LocalDate): Boolean {
    val base = baseOccurrenceDate(entry)
    if (date < base) return false
    if (!entry.repeatWeekly) return date == base
    return date.dayOfWeek.ordinal + 1 == entry.dayOfWeek
}

fun occurrenceStartInstant(entry: ScheduleEntry, date: LocalDate): Instant =
    date.atTime(LocalTime.parse(entry.startTime)).toInstant(TimeZone.of(entry.timeZoneId))

fun occurrenceEndInstant(entry: ScheduleEntry, date: LocalDate): Instant =
    date.atTime(LocalTime.parse(entry.endTime)).toInstant(TimeZone.of(entry.timeZoneId))

fun nextStartInstant(
    entry: ScheduleEntry,
    now: Instant = Clock.System.now(),
): Instant? {
    var date = baseOccurrenceDate(entry)
    var start = occurrenceStartInstant(entry, date)

    if (!entry.repeatWeekly) return start.takeIf { it > now }

    while (start <= now) {
        date = date.plus(DatePeriod(days = 7))
        start = occurrenceStartInstant(entry, date)
    }
    return start
}

fun nextReminderInstant(
    entry: ScheduleEntry,
    now: Instant = Clock.System.now(),
): Instant? {
    val start = nextStartInstant(entry, now) ?: return null
    val reminder = start - entry.reminderMinutes.minutes
    return when {
        reminder > now -> reminder
        start > now -> now + 1.seconds
        else -> null
    }
}

fun entriesForDate(entries: List<ScheduleEntry>, date: LocalDate): List<ScheduleEntry> =
    entries.filter { occursOn(it, date) }
        .sortedBy { it.startTime }

fun datesOfWeek(weekStart: LocalDate): List<LocalDate> =
    (0..6).map { weekStart.plus(DatePeriod(days = it)) }

fun dateLabel(date: LocalDate): String {
    val dayName = when (date.dayOfWeek.ordinal + 1) {
        1 -> "T2"
        2 -> "T3"
        3 -> "T4"
        4 -> "T5"
        5 -> "T6"
        6 -> "T7"
        else -> "CN"
    }
    return "$dayName ${date.day.toString().padStart(2, '0')}/${(date.month.ordinal + 1).toString().padStart(2, '0')}"
}

fun dayLabel(day: Int): String = when (day) {
    1 -> "T2"
    2 -> "T3"
    3 -> "T4"
    4 -> "T5"
    5 -> "T6"
    6 -> "T7"
    7 -> "CN"
    else -> "?"
}

fun formatInstantForEntry(entry: ScheduleEntry, instant: Instant): String {
    val local = instant.toLocalDateTime(TimeZone.of(entry.timeZoneId))
    return "${dateLabel(local.date)} ${local.time.hour.toString().padStart(2, '0')}:${local.time.minute.toString().padStart(2, '0')}"
}

fun withinNextWeek(instant: Instant, now: Instant = Clock.System.now()): Boolean =
    instant >= now && instant <= now + 7.days
