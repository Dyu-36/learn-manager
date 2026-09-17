package com.learnmanager.android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.learnmanager.platform.PlatformReminderScheduler

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        AndroidAppServices.initialize(context)
        val entryId = intent.getStringExtra(EXTRA_ENTRY_ID) ?: return
        val content = intent.getStringExtra(EXTRA_CONTENT) ?: "Lịch học"
        val type = intent.getStringExtra(EXTRA_TYPE) ?: ""
        val startTime = intent.getStringExtra(EXTRA_START_TIME) ?: ""

        AndroidNotification.show(
            context = context,
            entryId = entryId,
            content = content,
            type = type,
            startTime = startTime,
        )

        if (intent.getBooleanExtra(EXTRA_REPEAT_WEEKLY, false)) {
            val triggerAt = intent.getLongExtra(EXTRA_TRIGGER_AT, -1L)
            if (triggerAt > 0L) {
                PlatformReminderScheduler.scheduleNextRepeatFromReceiver(
                    entryId = entryId,
                    content = content,
                    type = type,
                    startTime = startTime,
                    triggerAtMillis = triggerAt + WEEK_MILLIS,
                )
            }
        }
    }

    companion object {
        const val EXTRA_ENTRY_ID = "entry_id"
        const val EXTRA_CONTENT = "content"
        const val EXTRA_TYPE = "type"
        const val EXTRA_START_TIME = "start_time"
        const val EXTRA_REPEAT_WEEKLY = "repeat_weekly"
        const val EXTRA_TRIGGER_AT = "trigger_at"
        private const val WEEK_MILLIS = 7L * 24L * 60L * 60L * 1000L
    }
}
