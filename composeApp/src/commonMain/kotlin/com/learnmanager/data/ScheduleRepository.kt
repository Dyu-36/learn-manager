package com.learnmanager.data

import com.learnmanager.model.AppState
import com.learnmanager.model.ScheduleEntry
import com.learnmanager.platform.PlatformReminderScheduler
import com.learnmanager.platform.PlatformStorage
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Clock

class ScheduleRepository(
    private val json: Json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    },
) {
    private val mutex = Mutex()

    suspend fun load(): AppState = mutex.withLock {
        val raw = PlatformStorage.read()
        val state = if (raw.isNullOrBlank()) {
            AppState()
        } else {
            runCatching { json.decodeFromString<AppState>(raw) }.getOrElse { AppState() }
        }
        PlatformReminderScheduler.reschedule(state.entries)
        state
    }

    suspend fun save(state: AppState): AppState = mutex.withLock {
        val normalized = state.copy(updatedAtEpochMillis = Clock.System.now().toEpochMilliseconds())
        PlatformStorage.write(json.encodeToString(normalized))
        PlatformReminderScheduler.reschedule(normalized.entries)
        normalized
    }

    suspend fun upsert(state: AppState, entry: ScheduleEntry): AppState {
        val entries = state.entries.toMutableList()
        val index = entries.indexOfFirst { it.id == entry.id }
        if (index >= 0) entries[index] = entry else entries += entry
        return save(state.copy(entries = entries))
    }

    suspend fun delete(state: AppState, entryId: String): AppState {
        PlatformReminderScheduler.cancel(entryId)
        return save(state.copy(entries = state.entries.filterNot { it.id == entryId }))
    }

    suspend fun rescheduleAll(): AppState {
        val state = load()
        PlatformReminderScheduler.reschedule(state.entries)
        return state
    }
}
