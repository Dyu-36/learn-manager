package com.learnmanager.android

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object AndroidNotification {
    private const val CHANNEL_ID = "learn_manager_schedule"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Nhắc lịch học",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Thông báo trước giờ học theo lịch LearnManager"
            }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    fun show(
        context: Context,
        entryId: String,
        content: String,
        type: String,
        startTime: String,
    ) {
        ensureChannel(context)
        val openIntent = Intent(context, MainActivity::class.java)
        val openPendingIntent = PendingIntent.getActivity(
            context,
            entryId.hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Sắp đến lịch: $content")
            .setContentText("Bắt đầu lúc $startTime • $type")
            .setStyle(NotificationCompat.BigTextStyle().bigText("$content bắt đầu lúc $startTime. Loại: $type"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(entryId.hashCode(), notification)
        }
    }
}
