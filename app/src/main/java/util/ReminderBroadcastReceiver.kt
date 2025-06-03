package util

// com.example.mindscribe.util/ReminderBroadcastReceiver.kt


import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.mindscribe.R // You will need to add an app icon in res/drawable
import com.example.mindscribe.MainActivity // Assuming your MainActivity is the entry point
import android.util.Log

private const val TAG = "ReminderReceiver"

class ReminderBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getIntExtra("reminder_id", -1)
        val title = intent.getStringExtra("reminder_title") ?: "Reminder"
        val description = intent.getStringExtra("reminder_description") ?: "You have a reminder."

        if (reminderId != -1) {
            Log.d(TAG, "Received reminder for ID: $reminderId, Title: $title")
            showNotification(context, reminderId, title, description)
        } else {
            Log.e(TAG, "Received reminder intent with no valid ID.")
        }
    }

    private fun showNotification(context: Context, reminderId: Int, title: String, description: String) {
        // Create an explicit intent for an Activity in your app
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification) // You'll need to create this drawable
            .setContentTitle(title)
            .setContentText(description)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent) // Set the intent that will fire when the user taps the notification
            .setAutoCancel(true) // Automatically dismiss the notification when the user taps it

        with(NotificationManagerCompat.from(context)) {
            // notificationId is a unique int for each notification that you must define
            notify(reminderId, builder.build())
            Log.d(TAG, "Notification shown for ID: $reminderId")
        }
    }
}