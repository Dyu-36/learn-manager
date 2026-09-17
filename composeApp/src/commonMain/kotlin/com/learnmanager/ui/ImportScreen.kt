package com.learnmanager.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.learnmanager.model.AppSettings
import com.learnmanager.parser.ScheduleDraft
import com.learnmanager.parser.ScheduleParseResult
import com.learnmanager.parser.ScheduleParser
import com.learnmanager.time.currentWeekStart
import com.learnmanager.time.dayLabel
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

@Composable
fun ImportScreen(
    settings: AppSettings,
    onImport: (List<ScheduleDraft>, LocalDate, String, Boolean) -> Unit,
) {
    var pastedText by remember {
        mutableStateOf("T2\t08:30-10:00\tCyber Security Certificate\tTự học\nT2\t10:15-11:45\tNhập môn ATTT\tPreview")
    }
    var weekStart by remember { mutableStateOf(currentWeekStart().toString()) }
    var timeZone by remember { mutableStateOf(TimeZone.currentSystemDefault().id) }
    var repeat by remember { mutableStateOf(true) }
    var result by remember { mutableStateOf<ScheduleParseResult?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Dán bảng TSV từ Notion / Excel / Google Sheets")
        Text("Mặc định nhắc ${settings.defaultReminderMinutes} phút trước mỗi lịch.")
        OutlinedTextField(
            value = pastedText,
            onValueChange = { pastedText = it },
            label = { Text("Ngày[TAB]Giờ[TAB]Nội dung[TAB]Loại") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 7,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(weekStart, { weekStart = it }, label = { Text("T2 bắt đầu") }, modifier = Modifier.weight(1f))
            OutlinedTextField(timeZone, { timeZone = it }, label = { Text("Time zone") }, modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Switch(checked = repeat, onCheckedChange = { repeat = it })
            Text("Lặp hàng tuần", modifier = Modifier.padding(top = 12.dp))
        }
        Button(onClick = {
            error = null
            result = ScheduleParser.parse(pastedText)
        }) { Text("Phân tích & xem trước") }

        error?.let { Text(it) }
        result?.let { parsed ->
            HorizontalDivider()
            Text("Hợp lệ: ${parsed.validRows.size} • Lỗi: ${parsed.invalidRows.size}")
            parsed.validRows.forEach { row ->
                Text("✓ ${dayLabel(row.dayOfWeek)} ${row.startTime}-${row.endTime} • ${row.content} • ${row.type}")
            }
            parsed.invalidRows.forEach { row ->
                Text("✗ Dòng ${row.lineNumber}: ${row.reason}\n${row.rawText}")
            }
            Button(
                enabled = parsed.validRows.isNotEmpty(),
                onClick = {
                    val date = runCatching { LocalDate.parse(weekStart) }.getOrNull()
                    val zoneValid = runCatching { TimeZone.of(timeZone) }.isSuccess
                    error = when {
                        date == null -> "Ngày bắt đầu tuần không hợp lệ"
                        !zoneValid -> "Time zone không hợp lệ"
                        else -> null
                    }
                    if (date != null && zoneValid) {
                        onImport(parsed.validRows, date, timeZone, repeat)
                    }
                },
            ) { Text("Import ${parsed.validRows.size} dòng hợp lệ") }
        }
    }
}
