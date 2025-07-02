package com.example.mindscribe.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.example.mindscribe.model.Reminder

class NotificationScheduler(private val context: Context) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val channelId = "reminder_channel"
    private val channelName = "Reminder Notifications"

    companion object {
        const val NOTIFICATION_TITLE = "notification_title"
        const val NOTIFICATION_TEXT = "notification_text"
        const val REMINDER_ID = "reminder_id"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for reminder notifications"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun scheduleReminder(reminder: Reminder) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !alarmManager.canScheduleExactAlarms()) {
            context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
            return
        }

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(NOTIFICATION_TITLE, reminder.title)
            putExtra(NOTIFICATION_TEXT, reminder.description ?: "You have a reminder")
            putExtra(REMINDER_ID, reminder.id)
            action = "ACTION_SHOW_REMINDER_${reminder.id}" // Unique action for each reminder
        }

        var flags = PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminder.timestamp,
                PendingIntent.getBroadcast(
                    context,
                    reminder.id,
                    intent,
                    flags
                )
            )
        } catch (e: SecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            }
        }
    }

    fun cancelReminder(reminderId: Int) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = "ACTION_SHOW_REMINDER_$reminderId" // Must match scheduled action
        }

        PendingIntent.getBroadcast(
            context,
            reminderId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        ).also { alarmManager.cancel(it) }
    }
}