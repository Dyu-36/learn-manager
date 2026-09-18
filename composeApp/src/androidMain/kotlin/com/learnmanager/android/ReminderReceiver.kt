package com.learnmanager.android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.learnmanager.model.ScheduleEntry
import com.learnmanager.platform.PlatformReminderScheduler

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        AndroidAppServices.initialize(context)
        val entry = ScheduleEntry(
            id = intent.getStringExtra(EXTRA_ENTRY_ID) ?: return,
            dayOfWeek = intent.getIntExtra(EXTRA_DAY_OF_WEEK, -1).takeIf { it in 1..7 } ?: return,
            startTime = intent.getStringExtra(EXTRA_START_TIME) ?: return,
            endTime = intent.getStringExtra(EXTRA_END_TIME) ?: return,
            content = intent.getStringExtra(EXTRA_CONTENT) ?: "Lịch học",
            type = intent.getStringExtra(EXTRA_TYPE) ?: "",
            weekStartDate = intent.getStringExtra(EXTRA_WEEK_START_DATE) ?: return,
            timeZoneId = intent.getStringExtra(EXTRA_TIME_ZONE_ID) ?: return,
            repeatWeekly = intent.getBooleanExtra(EXTRA_REPEAT_WEEKLY, false),
            reminderMinutes = intent.getIntExtra(EXTRA_REMINDER_MINUTES, 30),
        )

        AndroidNotification.show(
            context = context,
            entryId = entry.id,
            content = entry.content,
            type = entry.type,
            startTime = entry.startTime,
        )

        if (entry.repeatWeekly) {
            val previousReminderAt = intent.getLongExtra(EXTRA_TRIGGER_AT, -1L)
            if (previousReminderAt > 0L) {
                val referenceAfterStart = previousReminderAt + (entry.reminderMinutes + 1L) * 60_000L
                PlatformReminderScheduler.scheduleNextRepeatFromReceiver(
                    entry = entry,
                    referenceAfterStartMillis = referenceAfterStart,
                )
            }
        }
    }

    companion object {
        const val EXTRA_ENTRY_ID = "entry_id"
        const val EXTRA_DAY_OF_WEEK = "day_of_week"
        const val EXTRA_START_TIME = "start_time"
        const val EXTRA_END_TIME = "end_time"
        const val EXTRA_CONTENT = "content"
        const val EXTRA_TYPE = "type"
        const val EXTRA_WEEK_START_DATE = "week_start_date"
        const val EXTRA_TIME_ZONE_ID = "time_zone_id"
        const val EXTRA_REPEAT_WEEKLY = "repeat_weekly"
        const val EXTRA_REMINDER_MINUTES = "reminder_minutes"
        const val EXTRA_TRIGGER_AT = "trigger_at"
    }
}
