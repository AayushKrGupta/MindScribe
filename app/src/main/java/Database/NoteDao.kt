// Database/NoteDao.kt
package Database

import androidx.room.*
import backend.Note
import kotlinx.coroutines.flow.Flow // Import Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes")
    fun getAllNotes(): Flow<List<Note>> // Change to Flow

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Int): Note? // Keep as suspend for single lookup

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: Note)

    @Update
    suspend fun update(note: Note)

    @Delete
    suspend fun delete(note: Note)

    @Query("SELECT * FROM notes WHERE noteTitle LIKE '%' || :query || '%' OR noteDesc LIKE '%' || :query || '%'")
    fun searchNotes(query: String): Flow<List<Note>> // Change to Flow
}