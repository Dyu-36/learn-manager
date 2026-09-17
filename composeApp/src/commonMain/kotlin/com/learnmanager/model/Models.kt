package com.learnmanager.model

import kotlinx.serialization.Serializable
import kotlin.random.Random
import kotlin.time.Clock

@Serializable
data class ScheduleEntry(
    val id: String = newScheduleId(),
    val dayOfWeek: Int,
    val startTime: String,
    val endTime: String,
    val content: String,
    val type: String,
    val weekStartDate: String,
    val timeZoneId: String,
    val repeatWeekly: Boolean = true,
    val reminderMinutes: Int = 30,
    val updatedAtEpochMillis: Long = Clock.System.now().toEpochMilliseconds(),
)

@Serializable
data class AppSettings(
    val defaultReminderMinutes: Int = 30,
    val syncEnabled: Boolean = false,
    val supabaseUrl: String = "",
    val supabaseAnonKey: String = "",
    val syncCode: String = "",
    val lastSyncMessage: String = "Chưa đồng bộ",
    val lastSyncEpochMillis: Long? = null,
)

@Serializable
data class AppState(
    val schemaVersion: Int = 1,
    val entries: List<ScheduleEntry> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val updatedAtEpochMillis: Long = Clock.System.now().toEpochMilliseconds(),
)

@Serializable
data class SyncPayload(
    val entries: List<ScheduleEntry>,
    val defaultReminderMinutes: Int,
    val updatedAtEpochMillis: Long,
)

fun newScheduleId(): String = buildString {
    append(Clock.System.now().toEpochMilliseconds())
    append('-')
    append(Random.nextInt(100_000, 999_999))
}
