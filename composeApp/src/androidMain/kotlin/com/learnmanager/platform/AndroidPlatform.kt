package com.learnmanager.platform

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.learnmanager.android.AndroidAppServices
import com.learnmanager.android.ReminderReceiver
import com.learnmanager.model.ScheduleEntry
import com.learnmanager.time.nextReminderInstant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

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

    private fun stateFile(): File = File(AndroidAppServices.context().filesDir, "learn_manager_state.json")
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
    private val context: Context get() = AndroidAppServices.context()

    actual fun schedule(entry: ScheduleEntry) {
        val trigger = nextReminderInstant(entry) ?: return cancel(entry.id)
        scheduleAlarm(
            entryId = entry.id,
            content = entry.content,
            type = entry.type,
            startTime = entry.startTime,
            repeatWeekly = entry.repeatWeekly,
            triggerAtMillis = trigger.toEpochMilliseconds(),
        )
    }

    actual fun cancel(entryId: String) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = reminderAction(entryId)
        }
        val pending = PendingIntent.getBroadcast(
            context,
            entryId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
        if (pending != null) {
            alarmManager.cancel(pending)
            pending.cancel()
        }
    }

    actual fun reschedule(entries: List<ScheduleEntry>) {
        entries.forEach(::schedule)
    }

    actual fun capability(): NotificationCapability {
        val notificationsAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val exactAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        val detail = when {
            !notificationsAllowed -> "Android chưa cấp quyền thông báo."
            !exactAllowed -> "Thông báo đã bật; exact alarm chưa được cấp nên thời điểm nhắc có thể trễ."
            else -> "Thông báo và exact alarm đã sẵn sàng."
        }
        return NotificationCapability(notificationsAllowed, exactAllowed, detail)
    }

    actual fun requestExactAlarmAccess() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        if (alarmManager.canScheduleExactAlarms()) return
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun scheduleNextRepeatFromReceiver(
        entryId: String,
        content: String,
        type: String,
        startTime: String,
        triggerAtMillis: Long,
    ) {
        scheduleAlarm(
            entryId = entryId,
            content = content,
            type = type,
            startTime = startTime,
            repeatWeekly = true,
            triggerAtMillis = triggerAtMillis,
        )
    }

    private fun scheduleAlarm(
        entryId: String,
        content: String,
        type: String,
        startTime: String,
        repeatWeekly: Boolean,
        triggerAtMillis: Long,
    ) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pendingIntent = pendingIntent(
            entryId = entryId,
            content = content,
            type = type,
            startTime = startTime,
            repeatWeekly = repeatWeekly,
            triggerAtMillis = triggerAtMillis,
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    private fun pendingIntent(
        entryId: String,
        content: String,
        type: String,
        startTime: String,
        repeatWeekly: Boolean,
        triggerAtMillis: Long,
    ): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = reminderAction(entryId)
            putExtra(ReminderReceiver.EXTRA_ENTRY_ID, entryId)
            putExtra(ReminderReceiver.EXTRA_CONTENT, content)
            putExtra(ReminderReceiver.EXTRA_TYPE, type)
            putExtra(ReminderReceiver.EXTRA_START_TIME, startTime)
            putExtra(ReminderReceiver.EXTRA_REPEAT_WEEKLY, repeatWeekly)
            putExtra(ReminderReceiver.EXTRA_TRIGGER_AT, triggerAtMillis)
        }
        return PendingIntent.getBroadcast(
            context,
            entryId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun reminderAction(entryId: String): String = "com.learnmanager.REMINDER.$entryId"
}

actual object PlatformInfo {
    actual val name: String = "Android"
    actual val isDesktop: Boolean = false
}
