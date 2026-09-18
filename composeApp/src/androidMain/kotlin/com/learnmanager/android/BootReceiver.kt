package com.learnmanager.android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.learnmanager.data.ScheduleRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        AndroidAppServices.initialize(context)
        AndroidNotification.ensureChannel(context)
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                ScheduleRepository().rescheduleAll()
            } finally {
                pending.finish()
            }
        }
    }
}
