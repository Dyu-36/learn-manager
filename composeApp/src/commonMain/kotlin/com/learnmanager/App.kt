package com.learnmanager

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.learnmanager.controller.AppController
import com.learnmanager.model.ScheduleEntry
import com.learnmanager.platform.PlatformInfo
import com.learnmanager.time.currentWeekStart
import com.learnmanager.ui.ImportScreen
import com.learnmanager.ui.LearnManagerTheme
import com.learnmanager.ui.ScheduleEditorDialog
import com.learnmanager.ui.SettingsScreen
import com.learnmanager.ui.TodayScreen
import com.learnmanager.ui.WeekScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

private const val MESSAGE_AUTO_DISMISS_MS = 6_000L

private enum class MainScreen(val label: String) {
    TODAY("Hôm nay"),
    WEEK("Tuần"),
    IMPORT("Import"),
    SETTINGS("Cài đặt"),
}

@Composable
fun LearnManagerApp() {
    val controller = remember { AppController() }
    val scope = rememberCoroutineScope()
    var screen by remember { mutableStateOf(MainScreen.TODAY) }
    var editing by remember { mutableStateOf<ScheduleEntry?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<ScheduleEntry?>(null) }

    LaunchedEffect(Unit) {
        controller.initialize()
    }

    LaunchedEffect(controller.message) {
        if (controller.message != null) {
            delay(MESSAGE_AUTO_DISMISS_MS)
            controller.clearMessage()
        }
    }

    LearnManagerTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            if (controller.loading) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                    Text("Đang tải lịch...", modifier = Modifier.padding(top = 12.dp))
                }
                return@Surface
            }

            val screens = remember {
                if (PlatformInfo.isDesktop) MainScreen.entries.toList()
                else listOf(MainScreen.TODAY, MainScreen.WEEK, MainScreen.SETTINGS)
            }

            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text("LearnManager", style = MaterialTheme.typography.headlineSmall)
                        Text(PlatformInfo.name, style = MaterialTheme.typography.labelMedium)
                    }
                    Button(onClick = { creating = true }) {
                        Text("+ Thêm lịch")
                    }
                }

                TabRow(selectedTabIndex = screens.indexOf(screen).coerceAtLeast(0)) {
                    screens.forEach { item ->
                        Tab(
                            selected = screen == item,
                            onClick = { screen = item },
                            text = { Text(item.label) },
                        )
                    }
                }

                controller.message?.let { message ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(message, style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = controller::clearMessage) { Text("OK") }
                    }
                }

                when (screen) {
                    MainScreen.TODAY -> TodayScreen(
                        entries = controller.state.entries,
                        onEdit = { editing = it },
                        onDelete = { deleting = it },
                    )
                    MainScreen.WEEK -> WeekScreen(
                        entries = controller.state.entries,
                        onEdit = { editing = it },
                        onDelete = { deleting = it },
                    )
                    MainScreen.IMPORT -> ImportScreen(
                        settings = controller.state.settings,
                        onImport = { drafts, weekStart, timeZone, repeat ->
                            scope.launch { controller.importDrafts(drafts, weekStart, timeZone, repeat) }
                        },
                    )
                    MainScreen.SETTINGS -> SettingsScreen(
                        settings = controller.state.settings,
                        onSave = { settings -> scope.launch { controller.updateSettings(settings) } },
                        onPush = { scope.launch { controller.pushSync() } },
                        onPull = { scope.launch { controller.pullSync() } },
                    )
                }
            }

            if (creating) {
                val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                ScheduleEditorDialog(
                    initial = null,
                    defaultWeekStart = currentWeekStart().toString(),
                    defaultDay = now.date.dayOfWeek.ordinal + 1,
                    defaultTimeZone = TimeZone.currentSystemDefault().id,
                    defaultReminderMinutes = controller.state.settings.defaultReminderMinutes,
                    onDismiss = { creating = false },
                    onSave = { entry ->
                        creating = false
                        scope.launch { controller.saveEntry(entry) }
                    },
                )
            }

            editing?.let { entry ->
                ScheduleEditorDialog(
                    initial = entry,
                    defaultWeekStart = entry.weekStartDate,
                    defaultDay = entry.dayOfWeek,
                    defaultTimeZone = entry.timeZoneId,
                    defaultReminderMinutes = entry.reminderMinutes,
                    onDismiss = { editing = null },
                    onSave = { updated ->
                        editing = null
                        scope.launch { controller.saveEntry(updated) }
                    },
                )
            }

            deleting?.let { entry ->
                AlertDialog(
                    onDismissRequest = { deleting = null },
                    title = { Text("Xóa lịch?") },
                    text = { Text("\"${entry.content}\" (${entry.startTime}–${entry.endTime}) sẽ bị xóa và lời nhắc tương ứng bị hủy.") },
                    confirmButton = {
                        TextButton(onClick = {
                            deleting = null
                            scope.launch { controller.deleteEntry(entry.id) }
                        }) { Text("Xóa") }
                    },
                    dismissButton = {
                        TextButton(onClick = { deleting = null }) { Text("Hủy") }
                    },
                )
            }
        }
    }
}
