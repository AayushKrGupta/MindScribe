package util

// com.example.mindscribe.util/NotificationScheduler.kt


import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import Reminder.Reminder // Assuming Reminder data class is here
import android.annotation.SuppressLint
import java.util.Calendar
import java.util.Date

private const val TAG = "NotificationScheduler"
const val REMINDER_CHANNEL_ID = "mindscribe_reminder_channel"
const val REMINDER_CHANNEL_NAME = "MindScribe Reminders"
const val REMINDER_DESCRIPTION = "Notifications for your MindScribe notes and tasks."

class NotificationScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                REMINDER_CHANNEL_ID,
                REMINDER_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = REMINDER_DESCRIPTION
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created: $REMINDER_CHANNEL_NAME")
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    fun scheduleReminder(reminder: Reminder) {
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            putExtra("reminder_id", reminder.id)
            putExtra("reminder_title", reminder.title)
            putExtra("reminder_description", reminder.description)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id, // Use reminder ID as request code for uniqueness
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Set the alarm
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            reminder.timestamp,
            pendingIntent
        )
        Log.d(TAG, "Reminder scheduled for ${reminder.title} at ${Date(reminder.timestamp)}")
    }

    fun cancelReminder(reminderId: Int) {
        val intent = Intent(context, ReminderBroadcastReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE // Use FLAG_NO_CREATE to check if it exists
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel() // Also cancel the PendingIntent itself
            Log.d(TAG, "Reminder with ID $reminderId cancelled.")
        } else {
            Log.d(TAG, "Reminder with ID $reminderId not found to cancel.")
        }
    }
}