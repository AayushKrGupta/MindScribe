// Database/NoteDao.kt
package Database

import androidx.room.*
import backend.Note
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {



    @Query("SELECT * FROM notes WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAllNotesForUser(userId: String): Flow<List<Note>>
    @Query("SELECT * FROM notes WHERE id = :id AND userId = :userId")
    suspend fun getNoteByIdAndUser(id: String, userId: String): Note? // Changed id to String
    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getNoteById(id: String): Note? // Changed id to String
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: Note)
    @Update
    suspend fun update(note: Note)
    @Delete
    suspend fun delete(note: Note)

    @Query("SELECT * FROM notes WHERE userId = :userId AND (noteTitle LIKE '%' || :query || '%' OR noteDesc LIKE '%' || :query || '%')")
    fun searchNotesForUser(query: String, userId: String): Flow<List<Note>>
    @Query("DELETE FROM notes WHERE userId = :userId")
    suspend fun deleteAllNotesForUser(userId: String)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notes: List<Note>)
}