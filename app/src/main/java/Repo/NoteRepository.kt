package Repo

import Database.NoteDao
import android.util.Log
import backend.Note
import com.example.mindscribe.repository.FirestoreRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import javax.inject.Inject

class NoteRepository @Inject constructor(
    private val noteDao: NoteDao,
    private val firestoreRepo: FirestoreRepository
) {

    fun getAllNotesForUser(userId: String): Flow<List<Note>> = noteDao.getAllNotesForUser(userId)

    suspend fun getNoteByIdAndUser(id: String, userId: String): Note? = noteDao.getNoteByIdAndUser(id, userId)

    suspend fun getNoteById(id: String): Note? = noteDao.getNoteById(id)

    suspend fun insert(note: Note) {
        // Save locally first for immediate UI update
        noteDao.insert(note)

        // If logged in, sync to Firestore
        if (note.userId != "guest") {
            try {
                firestoreRepo.upsertNote(note, note.userId)
            } catch (e: Exception) {
                // If Firestore fails, keep the local version
                Log.e("NoteRepository", "Failed to sync note to Firestore", e)
            }
        }
    }

    suspend fun update(note: Note) {
        val updatedNote = note.copy(timestamp = System.currentTimeMillis())
        noteDao.update(updatedNote)

        if (note.userId != "guest") {
            try {
                firestoreRepo.upsertNote(updatedNote, note.userId)
            } catch (e: Exception) {
                Log.e("NoteRepository", "Failed to update note in Firestore", e)
            }
        }
    }

    suspend fun delete(note: Note) {
        noteDao.delete(note)

        if (note.userId != "guest") {
            try {
                firestoreRepo.deleteNote(note.id)
            } catch (e: Exception) {
                Log.e("NoteRepository", "Failed to delete note from Firestore", e)
            }
        }
    }

    fun searchNotesForUser(query: String, userId: String): Flow<List<Note>> =
        noteDao.searchNotesForUser(query, userId)

    suspend fun deleteAllNotesForUser(userId: String) = noteDao.deleteAllNotesForUser(userId)

    suspend fun getNoteImmediately(id: String, userId: String): Note? {
        return noteDao.getNoteByIdAndUser(id, userId)
    }

    suspend fun syncWithCloud(userId: String) {
        if (userId == "guest") return

        try {
            // Push local changes to cloud
            noteDao.getAllNotesForUser(userId).collect { localNotes ->
                localNotes.forEach { note ->
                    firestoreRepo.upsertNote(note, userId)
                }
            }

            // Pull cloud changes to local
            firestoreRepo.getNotesByUser(userId).collect { cloudNotes ->
                cloudNotes.forEach { note ->
                    // Only insert if note doesn't exist or is newer
                    val localNote = noteDao.getNoteById(note.id)
                    if (localNote == null || note.timestamp > localNote.timestamp) {
                        noteDao.insert(note)
                    }
                }
            }
        } catch (e: Exception) {
            throw Exception("Sync failed: ${e.message}")
        }
    }
}