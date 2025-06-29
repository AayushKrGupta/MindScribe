// Database/NoteDao.kt
package Database

import androidx.room.*
import backend.Note
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {


    // Get all notes for a specific user, ordered by timestamp
    @Query("SELECT * FROM notes WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAllNotesForUser(userId: String): Flow<List<Note>>

    // Get a specific note by ID, ensuring it belongs to the specified user
    @Query("SELECT * FROM notes WHERE id = :id AND userId = :userId")
    suspend fun getNoteByIdAndUser(id: String, userId: String): Note? // Changed id to String

    // NEW: Get a specific note by ID (without user ID, for general use in NoteScreen)
    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getNoteById(id: String): Note? // Changed id to String

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notes: List<Note>)
}