package backend

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.example.mindscribe.R
import java.util.Date

// Define your Note entity
@Entity(tableName = "notes") // Make sure your table name matches NoteDao queries
data class Note(
    @PrimaryKey
    var id: String = "", // Make it 'var' and provide a default empty string
    var userId: String = "", // Make it 'var' and provide a default empty string
    var noteTitle: String = "", // Make it 'var' and provide a default empty string
    var noteDesc: String = "", // Make it 'var' and provide a default empty string
    var timestamp: Long = System.currentTimeMillis(),
    var imageUrls: List<String>? = null,
    var audioPath: String? = null,
    var colorResId: Int = R.color.note_color_default,
    var isPinned: Boolean = false,
    var isArchived: Boolean = false
) {
    // Add an explicit no-argument constructor for Firebase Firestore
    // This will ensure Firestore can create an instance of Note before populating its fields.
    constructor() : this(
        "", // Default for id
        "", // Default for userId
        "", // Default for noteTitle
        "", // Default for noteDesc
        System.currentTimeMillis(), // Default for timestamp
        null, // Default for imageUrls
        null, // Default for audioPath
        R.color.note_color_default, // Default for colorResId
        false, // Default for isPinned
        false // Default for isArchived
    )
}