package backend

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.mindscribe.R
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val noteTitle: String,
    val noteDesc: String,
    val timestamp: Long = System.currentTimeMillis(),
    val imageUrls: List<String>? = null,
    val audioPath: String? = null,
    val isPinned: Boolean = false, // New field for pinning
    val isArchived: Boolean = false, // New field for archiving
    val colorResId: Int = com.example.mindscribe.R.color.note_color_default
) : Parcelable