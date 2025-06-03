// com.example.mindscribe.data/Reminder.kt
package Reminder

import androidx.room.Entity
import androidx.room.PrimaryKey

// Using Room annotations for future persistence, even if not fully implemented yet
@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val description: String?,
    val timestamp: Long // Milliseconds since epoch for the reminder time
)