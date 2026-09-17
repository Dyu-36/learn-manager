package com.learnmanager.controller

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.learnmanager.data.ScheduleRepository
import com.learnmanager.model.AppSettings
import com.learnmanager.model.AppState
import com.learnmanager.model.ScheduleEntry
import com.learnmanager.parser.ScheduleDraft
import com.learnmanager.sync.SupabaseSyncClient
import com.learnmanager.sync.SyncResult
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.time.Clock

class AppController(
    private val repository: ScheduleRepository = ScheduleRepository(),
    private val syncClient: SupabaseSyncClient = SupabaseSyncClient(),
) {
    var state by mutableStateOf(AppState())
        private set

    var loading by mutableStateOf(true)
        private set

    var message by mutableStateOf<String?>(null)
        private set

    suspend fun initialize() {
        loading = true
        state = repository.load()
        loading = false
    }

    fun clearMessage() {
        message = null
    }

    suspend fun saveEntry(entry: ScheduleEntry) {
        val stamped = entry.copy(updatedAtEpochMillis = Clock.System.now().toEpochMilliseconds())
        state = repository.upsert(state, stamped)
        message = "Đã lưu lịch"
    }

    suspend fun deleteEntry(entryId: String) {
        state = repository.delete(state, entryId)
        message = "Đã xóa lịch"
    }

    suspend fun importDrafts(
        drafts: List<ScheduleDraft>,
        weekStart: LocalDate,
        timeZoneId: String = TimeZone.currentSystemDefault().id,
        repeatWeekly: Boolean,
    ) {
        if (drafts.isEmpty()) {
            message = "Không có dòng hợp lệ để import"
            return
        }

        val reminder = state.settings.defaultReminderMinutes
        val newEntries = drafts.map { draft ->
            ScheduleEntry(
                dayOfWeek = draft.dayOfWeek,
                startTime = draft.startTime,
                endTime = draft.endTime,
                content = draft.content,
                type = draft.type,
                weekStartDate = weekStart.toString(),
                timeZoneId = timeZoneId,
                repeatWeekly = repeatWeekly,
                reminderMinutes = reminder,
            )
        }
        state = repository.save(state.copy(entries = state.entries + newEntries))
        message = "Đã import ${newEntries.size} lịch"
    }

    suspend fun updateSettings(settings: AppSettings) {
        state = repository.save(state.copy(settings = settings))
        message = "Đã lưu cài đặt"
    }

    suspend fun pushSync() {
        message = "Đang đẩy dữ liệu..."
        when (val result = syncClient.push(state)) {
            is SyncResult.Success -> updateSyncStatus(result.message)
            is SyncResult.Error -> updateSyncStatus(result.message)
        }
    }

    suspend fun pullSync() {
        message = "Đang tải dữ liệu..."
        when (val result = syncClient.pull(state.settings)) {
            is SyncResult.Error -> updateSyncStatus(result.message)
            is SyncResult.Success -> {
                val payload = result.payload ?: return updateSyncStatus(result.message)
                val updatedSettings = state.settings.copy(
                    defaultReminderMinutes = payload.defaultReminderMinutes,
                    lastSyncMessage = result.message,
                    lastSyncEpochMillis = Clock.System.now().toEpochMilliseconds(),
                )
                state = repository.save(
                    state.copy(
                        entries = payload.entries,
                        settings = updatedSettings,
                        updatedAtEpochMillis = payload.updatedAtEpochMillis,
                    ),
                )
                message = result.message
            }
        }
    }

    private suspend fun updateSyncStatus(status: String) {
        val settings = state.settings.copy(
            lastSyncMessage = status,
            lastSyncEpochMillis = Clock.System.now().toEpochMilliseconds(),
        )
        state = repository.save(state.copy(settings = settings))
        message = status
    }
}
