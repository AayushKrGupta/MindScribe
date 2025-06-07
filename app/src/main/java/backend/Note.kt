package backend

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.mindscribe.R
import java.util.*

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey
    var id: String = "",
    var userId: String = "",
    var noteTitle: String = "",
    var noteDesc: String = "",
    var timestamp: Long = System.currentTimeMillis(),
    var imageUrls: List<String>? = null,
    var audioPath: String? = null,
    var colorResId: Int = R.color.note_color_default,
    var isPinned: Boolean = false,
    var isArchived: Boolean = false
) {
    constructor() : this("", "", "", "", System.currentTimeMillis())

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Note

        if (id != other.id) return false
        if (userId != other.userId) return false
        if (noteTitle != other.noteTitle) return false
        if (noteDesc != other.noteDesc) return false
        if (timestamp != other.timestamp) return false
        if (isPinned != other.isPinned) return false
        if (isArchived != other.isArchived) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + userId.hashCode()
        result = 31 * result + noteTitle.hashCode()
        result = 31 * result + noteDesc.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + isPinned.hashCode()
        result = 31 * result + isArchived.hashCode()
        return result
    }
}

