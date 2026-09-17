package com.learnmanager.platform

import com.learnmanager.model.ScheduleEntry

data class NotificationCapability(
    val notificationsAllowed: Boolean,
    val exactAlarmsAllowed: Boolean,
    val detail: String,
)

data class PlatformHttpResponse(
    val statusCode: Int,
    val body: String,
)

expect object PlatformStorage {
    suspend fun read(): String?
    suspend fun write(content: String)
}

expect object PlatformHttp {
    suspend fun request(
        method: String,
        url: String,
        headers: Map<String, String> = emptyMap(),
        body: String? = null,
    ): PlatformHttpResponse
}

expect object PlatformReminderScheduler {
    fun schedule(entry: ScheduleEntry)
    fun cancel(entryId: String)
    fun reschedule(entries: List<ScheduleEntry>)
    fun capability(): NotificationCapability
    fun requestExactAlarmAccess()
}

expect object PlatformInfo {
    val name: String
    val isDesktop: Boolean
}
