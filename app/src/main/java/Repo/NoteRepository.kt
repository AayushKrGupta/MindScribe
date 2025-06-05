// Repo/NoteRepository.kt
package Repo

import Database.NoteDao
import backend.Note
import kotlinx.coroutines.flow.Flow

class NoteRepository(private val noteDao: NoteDao) {

    fun getAllNotesForUser(userId: String): Flow<List<Note>> {
        return noteDao.getAllNotesForUser(userId)
    }

    suspend fun getNoteByIdAndUser(id: String, userId: String): Note? { // Changed id to String
        return noteDao.getNoteByIdAndUser(id, userId)
    }

    suspend fun getNoteById(id: String): Note? { // Changed id to String
        return noteDao.getNoteById(id)
    }

    suspend fun insert(note: Note) {
        noteDao.insert(note)
    }

    suspend fun update(note: Note) {
        noteDao.update(note)
    }

    suspend fun delete(note: Note) {
        noteDao.delete(note)
    }

    fun searchNotesForUser(query: String, userId: String): Flow<List<Note>> {
        return noteDao.searchNotesForUser(query, userId)
    }

    suspend fun deleteAllNotesForUser(userId: String) {
        noteDao.deleteAllNotesForUser(userId)
    }
}