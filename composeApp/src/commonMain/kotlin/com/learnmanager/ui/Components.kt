package com.learnmanager.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.learnmanager.model.ScheduleEntry
import com.learnmanager.model.newScheduleId
import com.learnmanager.parser.ScheduleParser
import com.learnmanager.time.dayLabel
import com.learnmanager.time.formatInstantForEntry
import com.learnmanager.time.nextReminderInstant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.time.Clock

enum class EntryStatus { IN_PROGRESS, UPCOMING, NONE }

@Composable
fun ScheduleCard(
    entry: ScheduleEntry,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    status: EntryStatus = EntryStatus.NONE,
) {
    val nextReminder = remember(entry) { nextReminderInstant(entry) }

    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "${entry.startTime}–${entry.endTime} • ${entry.content}",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                when (status) {
                    EntryStatus.IN_PROGRESS -> Text(
                        "Đang diễn ra",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                    EntryStatus.UPCOMING -> Text(
                        "Tiếp theo",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    EntryStatus.NONE -> Unit
                }
            }
            Text(
                "${dayLabel(entry.dayOfWeek)} • ${entry.type} • nhắc ${entry.reminderMinutes} phút trước",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (nextReminder != null) {
                Text(
                    "Nhắc tiếp theo: ${formatInstantForEntry(entry, nextReminder)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onEdit) { Text("Sửa") }
                TextButton(onClick = onDelete) { Text("Xóa") }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScheduleEditorDialog(
    initial: ScheduleEntry?,
    defaultWeekStart: String,
    defaultDay: Int,
    defaultTimeZone: String,
    defaultReminderMinutes: Int,
    onDismiss: () -> Unit,
    onSave: (ScheduleEntry) -> Unit,
) {
    var day by remember(initial) { mutableStateOf(initial?.dayOfWeek ?: defaultDay.coerceIn(1, 7)) }
    var start by remember(initial) { mutableStateOf(initial?.startTime ?: "08:30") }
    var end by remember(initial) { mutableStateOf(initial?.endTime ?: "10:00") }
    var content by remember(initial) { mutableStateOf(initial?.content ?: "") }
    var type by remember(initial) { mutableStateOf(initial?.type ?: "") }
    var weekStart by remember(initial) { mutableStateOf(initial?.weekStartDate ?: defaultWeekStart) }
    var timeZone by remember(initial) { mutableStateOf(initial?.timeZoneId ?: defaultTimeZone) }
    var reminder by remember(initial) { mutableStateOf((initial?.reminderMinutes ?: defaultReminderMinutes).toString()) }
    var repeat by remember(initial) { mutableStateOf(initial?.repeatWeekly ?: true) }
    var error by remember(initial) { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Thêm lịch" else "Sửa lịch") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Thứ", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (1..7).forEach { value ->
                        FilterChip(
                            selected = day == value,
                            onClick = { day = value },
                            label = { Text(dayLabel(value)) },
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        start,
                        { start = it },
                        label = { Text("Bắt đầu (HH:MM)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        end,
                        { end = it },
                        label = { Text("Kết thúc (HH:MM)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                }
                OutlinedTextField(content, { content = it }, label = { Text("Nội dung") }, singleLine = true)
                OutlinedTextField(type, { type = it }, label = { Text("Loại") }, singleLine = true)
                OutlinedTextField(
                    weekStart,
                    { weekStart = it },
                    label = { Text("Ngày T2 bắt đầu tuần (YYYY-MM-DD)") },
                    singleLine = true,
                )
                OutlinedTextField(
                    timeZone,
                    { timeZone = it },
                    label = { Text("Múi giờ") },
                    singleLine = true,
                )
                OutlinedTextField(
                    reminder,
                    { reminder = it },
                    label = { Text("Nhắc trước (phút)") },
                    singleLine = true,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Switch(checked = repeat, onCheckedChange = { repeat = it })
                    Text("Lặp lại hàng tuần")
                }
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val reminderValue = reminder.toIntOrNull()
                val parsedTime = ScheduleParser.parseTimeRange("$start-$end")
                val validWeek = runCatching { LocalDate.parse(weekStart) }.isSuccess
                val validZone = runCatching { TimeZone.of(timeZone) }.isSuccess

                error = when {
                    parsedTime == null -> "Khoảng giờ không hợp lệ (ví dụ 08:30-10:00)"
                    content.isBlank() -> "Nội dung không được trống"
                    type.isBlank() -> "Loại không được trống"
                    !validWeek -> "Ngày bắt đầu tuần không hợp lệ"
                    !validZone -> "Múi giờ không hợp lệ"
                    reminderValue == null || reminderValue !in 0..1440 -> "Số phút nhắc phải từ 0 đến 1440"
                    else -> null
                }

                if (error == null) {
                    onSave(
                        ScheduleEntry(
                            id = initial?.id ?: newScheduleId(),
                            dayOfWeek = day,
                            startTime = parsedTime!!.first,
                            endTime = parsedTime.second,
                            content = content.trim(),
                            type = type.trim(),
                            weekStartDate = weekStart,
                            timeZoneId = timeZone,
                            repeatWeekly = repeat,
                            reminderMinutes = reminderValue!!,
                            updatedAtEpochMillis = Clock.System.now().toEpochMilliseconds(),
                        ),
                    )
                }
            }) { Text("Lưu") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } },
    )
}
