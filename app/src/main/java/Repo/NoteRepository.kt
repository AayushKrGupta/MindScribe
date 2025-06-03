// Repo/NoteRepository.kt
package Repo

import Database.NoteDao
import backend.Note
import kotlinx.coroutines.flow.Flow // Import Flow

class NoteRepository(private val noteDao: NoteDao) {

    // This should now be a Flow from your DAO
    val allNotes: Flow<List<Note>> = noteDao.getAllNotes()

    suspend fun insert(note: Note) {
        noteDao.insert(note)
    }

    suspend fun update(note: Note) {
        noteDao.update(note)
    }

    suspend fun delete(note: Note) {
        noteDao.delete(note)
    }

    suspend fun getNoteById(id: Int): Note? {
        return noteDao.getNoteById(id)
    }

    // Assuming NoteDao also has a search method that returns a Flow
    fun searchNotes(query: String): Flow<List<Note>> {
        return noteDao.searchNotes(query) // Ensure your DAO has this method
    }
}