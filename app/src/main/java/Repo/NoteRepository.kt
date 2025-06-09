package Repo

import Database.NoteDao
import backend.Note
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class NoteRepository @Inject constructor(private val noteDao: NoteDao) {

    fun getAllNotesForUser(userId: String): Flow<List<Note>> = noteDao.getAllNotesForUser(userId)

    suspend fun getNoteByIdAndUser(id: String, userId: String): Note? = noteDao.getNoteByIdAndUser(id, userId)

    suspend fun getNoteById(id: String): Note? = noteDao.getNoteById(id)

    // Modified insert function to ensure immediate UI update
    suspend fun insert(note: Note) {
        noteDao.insert(note)
        // Force Room to recognize the change by querying the inserted note
        noteDao.getNoteById(note.id)?.let {
            // This triggers the Flow to emit the updated list
        }
    }

    // Enhanced update function
    suspend fun update(note: Note) {
        noteDao.update(note.copy(timestamp = System.currentTimeMillis()))
        // Force update recognition
        noteDao.getNoteById(note.id)
    }

    // Enhanced delete function
    suspend fun delete(note: Note) {
        noteDao.delete(note)
        // Force update recognition by querying a different note
        noteDao.getAllNotesForUser(note.userId).collect { /* triggers flow update */ }
    }

    fun searchNotesForUser(query: String, userId: String): Flow<List<Note>> =
        noteDao.searchNotesForUser(query, userId)

    suspend fun deleteAllNotesForUser(userId: String) = noteDao.deleteAllNotesForUser(userId)

    // New function to force immediate refresh
    suspend fun getNoteImmediately(id: String, userId: String): Note? {
        return noteDao.getNoteByIdAndUser(id, userId)
    }
}