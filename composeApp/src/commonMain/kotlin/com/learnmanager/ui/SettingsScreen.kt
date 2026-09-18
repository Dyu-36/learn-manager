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
import androidx.compose.material3.MaterialTheme
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
import com.learnmanager.platform.PlatformReminderScheduler
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onSave: (AppSettings) -> Unit,
    onPush: () -> Unit,
    onPull: () -> Unit,
) {
    var reminder by remember(settings) { mutableStateOf(settings.defaultReminderMinutes.toString()) }
    var syncEnabled by remember(settings) { mutableStateOf(settings.syncEnabled) }
    var supabaseUrl by remember(settings) { mutableStateOf(settings.supabaseUrl) }
    var anonKey by remember(settings) { mutableStateOf(settings.supabaseAnonKey) }
    var syncCode by remember(settings) { mutableStateOf(settings.syncCode) }
    var error by remember(settings) { mutableStateOf<String?>(null) }
    val capability = PlatformReminderScheduler.capability()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Thông báo")
        Text(capability.detail)
        if (!capability.exactAlarmsAllowed) {
            Button(onClick = { PlatformReminderScheduler.requestExactAlarmAccess() }) {
                Text("Cho phép báo đúng giờ")
            }
        }
        OutlinedTextField(
            value = reminder,
            onValueChange = { reminder = it },
            label = { Text("Mặc định nhắc trước (phút)") },
            singleLine = true,
        )

        Text("Đồng bộ Supabase")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Switch(checked = syncEnabled, onCheckedChange = { syncEnabled = it })
            Text(if (syncEnabled) "Đang bật" else "Đang tắt", modifier = Modifier.padding(top = 12.dp))
        }
        OutlinedTextField(supabaseUrl, { supabaseUrl = it }, label = { Text("Supabase URL") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(anonKey, { anonKey = it }, label = { Text("Supabase anon key") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(syncCode, { syncCode = it }, label = { Text("Mã đồng bộ dùng chung") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Text("Trạng thái: ${settings.lastSyncMessage}")
        settings.lastSyncEpochMillis?.let { millis ->
            val local = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault())
            Text(
                "Lần cuối: %02d/%02d %02d:%02d".format(
                    local.date.dayOfMonth,
                    local.date.monthNumber,
                    local.time.hour,
                    local.time.minute,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Button(onClick = {
            val minutes = reminder.toIntOrNull()
            error = if (minutes == null || minutes !in 0..1440) "Số phút nhắc không hợp lệ" else null
            if (minutes != null && minutes in 0..1440) {
                onSave(
                    settings.copy(
                        defaultReminderMinutes = minutes,
                        syncEnabled = syncEnabled,
                        supabaseUrl = supabaseUrl.trim(),
                        supabaseAnonKey = anonKey.trim(),
                        syncCode = syncCode.trim(),
                    ),
                )
            }
        }) { Text("Lưu cài đặt") }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = syncEnabled, onClick = onPush) { Text("Push") }
            Button(enabled = syncEnabled, onClick = onPull) { Text("Pull") }
        }
    }
}
