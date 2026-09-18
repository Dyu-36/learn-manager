package com.learnmanager.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.learnmanager.model.ScheduleEntry
import com.learnmanager.time.currentWeekStart
import com.learnmanager.time.dateLabel
import com.learnmanager.time.datesOfWeek
import com.learnmanager.time.entriesForDate
import com.learnmanager.time.occurrenceEndInstant
import com.learnmanager.time.occurrenceStartInstant
import kotlinx.coroutines.delay
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

private const val REFRESH_INTERVAL_MS = 30_000L

/** Re-computes [now] every 30 seconds so "today", in-progress badges, and week views stay current. */
@Composable
private fun rememberNow(): Instant {
    var now by remember { mutableStateOf(Clock.System.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(REFRESH_INTERVAL_MS)
            now = Clock.System.now()
        }
    }
    return now
}

@Composable
fun TodayScreen(
    entries: List<ScheduleEntry>,
    onEdit: (ScheduleEntry) -> Unit,
    onDelete: (ScheduleEntry) -> Unit,
) {
    val zone = TimeZone.currentSystemDefault()
    val now = rememberNow()
    val today = now.toLocalDateTime(zone).date
    val todayEntries = entriesForDate(entries, today)
    val nextUpId = todayEntries.firstOrNull { occurrenceStartInstant(it, today) > now }?.id

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(dateLabel(today), style = MaterialTheme.typography.titleLarge)
        if (todayEntries.isEmpty()) {
            Text("Không có lịch hôm nay.", modifier = Modifier.padding(top = 12.dp))
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
                items(todayEntries, key = { it.id }) { entry ->
                    val status = when {
                        occurrenceStartInstant(entry, today) <= now && now < occurrenceEndInstant(entry, today) ->
                            EntryStatus.IN_PROGRESS
                        entry.id == nextUpId -> EntryStatus.UPCOMING
                        else -> EntryStatus.NONE
                    }
                    ScheduleCard(
                        entry = entry,
                        status = status,
                        onEdit = { onEdit(entry) },
                        onDelete = { onDelete(entry) },
                    )
                }
            }
        }
    }
}

@Composable
fun WeekScreen(
    entries: List<ScheduleEntry>,
    onEdit: (ScheduleEntry) -> Unit,
    onDelete: (ScheduleEntry) -> Unit,
) {
    val thisWeekStart = currentWeekStart()
    var weekStart by remember { mutableStateOf(thisWeekStart) }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = { weekStart = weekStart.plus(DatePeriod(days = -7)) }) { Text("‹ Tuần trước") }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Tuần ${dateLabel(weekStart)}",
                    style = MaterialTheme.typography.titleMedium,
                )
                if (weekStart != thisWeekStart) {
                    OutlinedButton(onClick = { weekStart = thisWeekStart }) { Text("Về tuần này") }
                }
            }
            TextButton(onClick = { weekStart = weekStart.plus(DatePeriod(days = 7)) }) { Text("Tuần sau ›") }
        }

        val dates = datesOfWeek(weekStart)
        LazyColumn(modifier = Modifier.fillMaxSize().padding(bottom = 12.dp)) {
            dates.forEach { date ->
                item(key = "header-$date") {
                    Text(
                        dateLabel(date),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
                    )
                    HorizontalDivider()
                }
                val dayEntries = entriesForDate(entries, date)
                if (dayEntries.isEmpty()) {
                    item(key = "empty-$date") {
                        Text(
                            "Trống",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }
                } else {
                    items(dayEntries, key = { "${date}-${it.id}" }) { entry ->
                        ScheduleCard(entry, onEdit = { onEdit(entry) }, onDelete = { onDelete(entry) })
                    }
                }
            }
        }
    }
}
