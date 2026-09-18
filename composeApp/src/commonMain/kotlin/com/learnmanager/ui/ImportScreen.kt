package com.learnmanager.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.learnmanager.model.AppSettings
import com.learnmanager.parser.ScheduleDraft
import com.learnmanager.parser.ScheduleParseResult
import com.learnmanager.parser.ScheduleParser
import com.learnmanager.time.currentWeekStart
import com.learnmanager.time.dateLabel
import com.learnmanager.time.dayLabel
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus

@Composable
fun ImportScreen(
    settings: AppSettings,
    onImport: (List<ScheduleDraft>, LocalDate, String, Boolean) -> Unit,
) {
    var pastedText by remember { mutableStateOf("") }
    var weekStart by remember { mutableStateOf(currentWeekStart().toString()) }
    var timeZone by remember { mutableStateOf(TimeZone.currentSystemDefault().id) }
    var repeat by remember { mutableStateOf(true) }
    var result by remember { mutableStateOf<ScheduleParseResult?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Import lịch", style = MaterialTheme.typography.titleMedium)
        Text(
            "Mỗi dòng theo mẫu: T2  08:30-10:00  Cyber Security Certificate  Tự học. " +
                "Dùng hai dấu cách giữa 4 phần, không cần thêm dấu phân cách. Dữ liệu dán từ Excel/Notion bằng TAB vẫn được nhận.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = pastedText,
            onValueChange = {
                pastedText = it
                result = null
                error = null
            },
            label = { Text("Dán lịch vào đây") },
            placeholder = { Text("T2  08:30-10:00  Cyber Security Certificate  Tự học") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 7,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                enabled = pastedText.isNotBlank(),
                onClick = {
                    pastedText = ScheduleParser.format(pastedText)
                    result = ScheduleParser.parse(pastedText)
                    error = null
                },
            ) { Text("Format lại") }
            Button(
                enabled = pastedText.isNotBlank(),
                onClick = {
                    error = null
                    result = ScheduleParser.parse(pastedText)
                },
            ) { Text("Phân tích & xem trước") }
        }
        Text(
            "Mặc định nhắc ${settings.defaultReminderMinutes} phút trước mỗi lịch.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
            OutlinedTextField(
                weekStart,
                { weekStart = it },
                label = { Text("Ngày T2 bắt đầu tuần (YYYY-MM-DD)") },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(bottom = 4.dp)) {
                OutlinedButton(onClick = { weekStart = currentWeekStart().toString() }) { Text("Tuần này") }
                OutlinedButton(onClick = {
                    weekStart = currentWeekStart().plus(DatePeriod(days = 7)).toString()
                }) { Text("Tuần sau") }
            }
        }
        OutlinedTextField(
            timeZone,
            { timeZone = it },
            label = { Text("Múi giờ (mặc định theo thiết bị)") },
            modifier = Modifier.fillMaxWidth(),
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

        result?.let { parsed ->
            HorizontalDivider()
            val weekStartLabel = runCatching { LocalDate.parse(weekStart) }
                .map { dateLabel(it) }
                .getOrDefault(weekStart)
            Text(
                "Hợp lệ: ${parsed.validRows.size} • Lỗi: ${parsed.invalidRows.size} • Áp dụng từ $weekStartLabel",
                style = MaterialTheme.typography.titleSmall,
            )
            parsed.validRows.forEach { row ->
                Text(
                    "✓ ${dayLabel(row.dayOfWeek)} ${row.startTime}-${row.endTime} • ${row.content} • ${row.type}",
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            parsed.invalidRows.forEach { row ->
                Text(
                    "✗ Dòng ${row.lineNumber}: ${row.reason}\n${row.rawText}",
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Button(
                enabled = parsed.validRows.isNotEmpty(),
                onClick = {
                    val date = runCatching { LocalDate.parse(weekStart) }.getOrNull()
                    val zoneValid = runCatching { TimeZone.of(timeZone) }.isSuccess
                    error = when {
                        date == null -> "Ngày bắt đầu tuần không hợp lệ"
                        !zoneValid -> "Múi giờ không hợp lệ"
                        else -> null
                    }
                    if (date != null && zoneValid) {
                        onImport(parsed.validRows, date, timeZone, repeat)
                        pastedText = ""
                        result = null
                    }
                },
            ) { Text("Import ${parsed.validRows.size} dòng hợp lệ") }
        }
    }
}
