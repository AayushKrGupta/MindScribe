// Database/NoteDao.kt
package Database

import androidx.room.*
import backend.Note
import kotlinx.coroutines.flow.Flow // Import Flow

@Dao
interface NoteDao {
    // Get all notes for a specific user, ordered by timestamp
    @Query("SELECT * FROM notes WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAllNotesForUser(userId: String): Flow<List<Note>>

    // Get a specific note by ID, ensuring it belongs to the specified user
    @Query("SELECT * FROM notes WHERE id = :id AND userId = :userId")
    suspend fun getNoteByIdAndUser(id: Int, userId: String): Note?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: Note)

    @Update
    suspend fun update(note: Note)

    @Delete
    suspend fun delete(note: Note)

    // Search notes for a specific user, filtering by title or description
    @Query("SELECT * FROM notes WHERE userId = :userId AND (noteTitle LIKE '%' || :query || '%' OR noteDesc LIKE '%' || :query || '%')")
    fun searchNotesForUser(query: String, userId: String): Flow<List<Note>>

    // Optional: Add a method to delete all notes for a specific user
    @Query("DELETE FROM notes WHERE userId = :userId")
    suspend fun deleteAllNotesForUser(userId: String)
}