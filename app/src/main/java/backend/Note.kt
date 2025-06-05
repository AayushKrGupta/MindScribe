// backend/Note.kt (Example - adjust based on your actual Note class structure)
package backend

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.example.mindscribe.R
import java.util.Date

// Define your Note entity
@Entity(tableName = "notes") // Make sure your table name matches NoteDao queries
data class Note(
    @PrimaryKey // Room needs a primary key
    val id: String, // CHANGE THIS TO STRING
    val userId: String, // Firebase user ID
    val noteTitle: String,
    val noteDesc: String,
    val timestamp: Long = System.currentTimeMillis(),
    val imageUrls: List<String>? = null, // List of image URI strings
    val audioPath: String? = null,
    val colorResId: Int = R.color.note_color_default, // Default color resource ID
    val isPinned: Boolean = false,
    val isArchived: Boolean = false
)

