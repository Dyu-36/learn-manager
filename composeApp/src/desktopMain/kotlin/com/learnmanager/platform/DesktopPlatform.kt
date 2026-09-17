package com.learnmanager.platform

import com.learnmanager.desktop.DesktopTray
import com.learnmanager.model.ScheduleEntry
import com.learnmanager.time.nextReminderInstant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

actual object PlatformStorage {
    actual suspend fun read(): String? = withContext(Dispatchers.IO) {
        val file = stateFile()
        if (file.exists()) file.readText() else null
    }

    actual suspend fun write(content: String) = withContext(Dispatchers.IO) {
        val file = stateFile()
        file.parentFile?.mkdirs()
        file.writeText(content)
    }

    private fun stateFile(): File = File(System.getProperty("user.home"), ".learn-manager/state.json")
}

actual object PlatformHttp {
    actual suspend fun request(
        method: String,
        url: String,
        headers: Map<String, String>,
        body: String?,
    ): PlatformHttpResponse = withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            headers.forEach { (name, value) -> connection.setRequestProperty(name, value) }
            if (body != null) {
                connection.doOutput = true
                connection.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(body) }
            }
            val code = connection.responseCode
            val stream = if (code in 200..399) connection.inputStream else connection.errorStream
            val responseBody = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            PlatformHttpResponse(code, responseBody)
        } finally {
            connection.disconnect()
        }
    }
}

actual object PlatformReminderScheduler {
    private val timer = Timer("learn-manager-reminders", true)
    private val tasks = ConcurrentHashMap<String, TimerTask>()

    actual fun schedule(entry: ScheduleEntry) {
        cancel(entry.id)
        scheduleFrom(entry, Clock.System.now())
    }

    private fun scheduleFrom(entry: ScheduleEntry, reference: kotlin.time.Instant) {
        val target = nextReminderInstant(entry, reference) ?: return
        val delay = max(0L, target.toEpochMilliseconds() - Clock.System.now().toEpochMilliseconds())
        val task = object : TimerTask() {
            override fun run() {
                DesktopTray.notify(
                    title = "Sắp đến lịch: ${entry.content}",
                    message = "Bắt đầu lúc ${entry.startTime} • ${entry.type}",
                )
                tasks.remove(entry.id)
                if (entry.repeatWeekly) {
                    scheduleFrom(entry, Clock.System.now() + (entry.reminderMinutes + 1).minutes)
                }
            }
        }
        tasks[entry.id] = task
        timer.schedule(task, delay)
    }

    actual fun cancel(entryId: String) {
        tasks.remove(entryId)?.cancel()
    }

    actual fun reschedule(entries: List<ScheduleEntry>) {
        val activeIds = entries.mapTo(mutableSetOf()) { it.id }
        tasks.keys.filterNot { it in activeIds }.forEach(::cancel)
        entries.forEach(::schedule)
    }

    actual fun capability(): NotificationCapability = NotificationCapability(
        notificationsAllowed = true,
        exactAlarmsAllowed = true,
        detail = "Desktop dùng system tray; hãy để LearnManager chạy nền để nhận nhắc lịch.",
    )

    actual fun requestExactAlarmAccess() = Unit
}

actual object PlatformInfo {
    actual val name: String = "Windows / Desktop"
    actual val isDesktop: Boolean = true
}
