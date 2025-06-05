// Repo/NoteRepository.kt
package Repo

import Database.NoteDao // Ensure NoteDao is correctly imported from its package
import backend.Note
import kotlinx.coroutines.flow.Flow // Import Flow

class NoteRepository(private val noteDao: NoteDao) {

    // Removed the 'val allNotes' property as it is now user-specific and accessed via a function.

    /**
     * Retrieves a Flow of all notes for a specific user from the database.
     * @param userId The ID of the user whose notes to retrieve.
     * @return A Flow emitting a list of Note objects for the given user.
     */
    fun getAllNotesForUser(userId: String): Flow<List<Note>> {
        return noteDao.getAllNotesForUser(userId)
    }

    /**
     * Retrieves a specific note by its ID and the user ID.
     * Ensures that a user can only retrieve their own notes.
     * @param id The ID of the note.
     * @param userId The ID of the user trying to retrieve the note.
     * @return The Note object if found and belongs to the user, otherwise null.
     */
    suspend fun getNoteByIdAndUser(id: Int, userId: String): Note? {
        return noteDao.getNoteByIdAndUser(id, userId)
    }

    /**
     * Inserts a new note into the database.
     * The Note object provided should already contain the correct userId.
     * @param note The Note object to insert.
     */
    suspend fun insert(note: Note) {
        noteDao.insert(note)
    }

    /**
     * Updates an existing note in the database.
     * The Note object provided should already contain the correct userId and its identity should be verified by the ViewModel.
     * @param note The Note object to update.
     */
    suspend fun update(note: Note) {
        noteDao.update(note)
    }

    /**
     * Deletes a note from the database.
     * The Note object provided should already contain the correct userId and its identity should be verified by the ViewModel.
     * @param note The Note object to delete.
     */
    suspend fun delete(note: Note) {
        noteDao.delete(note)
    }

    /**
     * Searches for notes belonging to a specific user based on a query string.
     * @param query The search string (case-insensitive, partial match).
     * @param userId The ID of the user whose notes to search.
     * @return A Flow emitting a list of matching Note objects for the given user.
     */
    fun searchNotesForUser(query: String, userId: String): Flow<List<Note>> {
        return noteDao.searchNotesForUser(query, userId)
    }

    /**
     * Deletes all notes associated with a specific user.
     * This can be useful for account management features like account deletion.
     * @param userId The ID of the user whose notes should be deleted.
     */
    suspend fun deleteAllNotesForUser(userId: String) {
        noteDao.deleteAllNotesForUser(userId)
    }
}