package com.example.mindscribe.util

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.mindscribe.MainActivity
import com.example.mindscribe.R
import com.example.mindscribe.data.ReminderDatabase
import com.example.mindscribe.repository.ReminderRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> handleBootComplete(context)
            else -> handleReminderNotification(context, intent)
        }
    }

    private fun handleReminderNotification(context: Context, intent: Intent) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

        // Get reminder details from intent using the *correct* constants/keys
        // IMPORTANTE: Use the constants defined in NotificationScheduler
        val title = intent.getStringExtra(NotificationScheduler.NOTIFICATION_TITLE) ?: "Reminder"
        val text = intent.getStringExtra(NotificationScheduler.NOTIFICATION_TEXT) ?: "You have a reminder"
        val reminderId = intent.getIntExtra(NotificationScheduler.REMINDER_ID, 0) // Also good to use the constant here

        // Create intent to open app when notification is tapped
        val notificationIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("NOTIFICATION_ID", reminderId) // This extra is for MainActivity if it needs it
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            reminderId, // Use reminder ID as request code to make each pending intent unique
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Build notification with actual reminder content
        val notification = NotificationCompat.Builder(context, "reminder_channel")
            .setSmallIcon(com.example.mindscribe.R.drawable.ic_notification)
            .setContentTitle(title) // Now this will use the actual reminder title
            .setContentText(text)   // Now this will use the actual reminder description
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text)) // Show full description
            .build()

        notificationManager.notify(reminderId, notification)
    }

    private fun handleBootComplete(context: Context) {
        val repository = ReminderRepository(
            ReminderDatabase.getDatabase(context).reminderDao()
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                repository.allReminders.collect { reminders ->
                    val scheduler = NotificationScheduler(context)
                    reminders.forEach { reminder ->
                        if (reminder.timestamp > System.currentTimeMillis()) {
                            scheduler.scheduleReminder(reminder)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}