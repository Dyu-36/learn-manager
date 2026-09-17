package com.learnmanager.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.learnmanager.model.ScheduleEntry
import com.learnmanager.time.currentWeekStart
import com.learnmanager.time.dateLabel
import com.learnmanager.time.datesOfWeek
import com.learnmanager.time.entriesForDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

@Composable
fun TodayScreen(
    entries: List<ScheduleEntry>,
    onEdit: (ScheduleEntry) -> Unit,
    onDelete: (ScheduleEntry) -> Unit,
) {
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val todayEntries = entriesForDate(entries, today)

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("${dateLabel(today)}", style = MaterialTheme.typography.titleLarge)
        if (todayEntries.isEmpty()) {
            Text("Không có lịch hôm nay.", modifier = Modifier.padding(top = 12.dp))
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
                items(todayEntries, key = { it.id }) { entry ->
                    ScheduleCard(entry, onEdit = { onEdit(entry) }, onDelete = { onDelete(entry) })
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
    val weekStart = currentWeekStart()
    val dates = datesOfWeek(weekStart)

    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
        dates.forEach { date ->
            item(key = "header-$date") {
                Text(dateLabel(date), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))
                HorizontalDivider()
            }
            val dayEntries = entriesForDate(entries, date)
            if (dayEntries.isEmpty()) {
                item(key = "empty-$date") {
                    Text("Trống", modifier = Modifier.padding(vertical = 8.dp))
                }
            } else {
                items(dayEntries, key = { "${date}-${it.id}" }) { entry ->
                    ScheduleCard(entry, onEdit = { onEdit(entry) }, onDelete = { onDelete(entry) })
                }
            }
        }
    }
}
